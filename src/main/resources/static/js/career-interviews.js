(function () {
    const userId = window.TUMOOH_CAREER && window.TUMOOH_CAREER.userId;
    if (!userId) {
        return;
    }

    const CELL = 'py-3 px-4 align-middle';

    function formatInterviewDate(value) {
        if (value == null || value === '') {
            return '—';
        }
        var d;
        if (Array.isArray(value)) {
            d = new Date(value[0], value[1] - 1, value[2], value[3] || 0, value[4] || 0);
        } else {
            d = new Date(String(value).trim().replace(' ', 'T'));
        }
        if (Number.isNaN(d.getTime())) {
            return String(value);
        }
        return d.toLocaleString(undefined, {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: 'numeric',
            minute: '2-digit'
        });
    }

    function escapeHtml(text) {
        return String(text == null ? '' : text)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    const tableBody = document.getElementById('interviews-table-body');
    const errorEl = document.getElementById('career-error');
    const statusFilter = document.getElementById('status-filter');

    function bindRowActions() {
        if (!tableBody) {
            return;
        }
        tableBody.querySelectorAll('.save-status').forEach(function (btn) {
            btn.addEventListener('click', async function () {
                const id = btn.getAttribute('data-id');
                const select = tableBody.querySelector('select[data-id="' + id + '"]');
                try {
                    await window.TumoohApi.patch('/interviews/' + id + '/status', { status: select.value });
                    btn.textContent = 'Saved';
                } catch (err) {
                    alert(err.message);
                }
            });
        });
    }

    async function loadInterviews() {
        try {
            let path = '/users/' + userId + '/interviews/upcoming';
            const status = statusFilter && statusFilter.value;
            if (status) {
                path += '?status=' + encodeURIComponent(status);
            }
            const rows = await window.TumoohApi.get(path);
            if (!tableBody) {
                return;
            }
            if (!rows.length) {
                tableBody.innerHTML =
                    '<tr><td colspan="5" class="py-10 px-4 text-center text-tumooh-navy/60">No upcoming interviews.</td></tr>';
                return;
            }
            tableBody.innerHTML = rows.map(function (row) {
                var role = escapeHtml(row.jobPosition || '—');
                return '<tr class="border-t border-tumooh-navy/10">' +
                    '<td class="' + CELL + ' tabular-nums">' + row.interviewId + '</td>' +
                    '<td class="' + CELL + ' font-medium">' + role + '</td>' +
                    '<td class="' + CELL + ' whitespace-nowrap text-tumooh-navy/80">' + escapeHtml(formatInterviewDate(row.interviewDate)) + '</td>' +
                    '<td class="' + CELL + '">' +
                    '<select data-id="' + row.interviewId + '" class="status-select w-full max-w-[9.25rem] rounded-lg border border-tumooh-navy/15 px-2 py-1.5">' +
                    ['SCHEDULED', 'COMPLETED', 'CANCELLED'].map(function (st) {
                        return '<option value="' + st + '"' + (row.status === st ? ' selected' : '') + '>' + st + '</option>';
                    }).join('') +
                    '</select></td>' +
                    '<td class="' + CELL + ' text-right whitespace-nowrap">' +
                    '<button type="button" class="save-status text-tumooh-accent font-medium" data-id="' + row.interviewId + '">Save</button>' +
                    '</td></tr>';
            }).join('');

            bindRowActions();
        } catch (err) {
            if (errorEl) {
                errorEl.textContent = err.message;
                errorEl.classList.remove('hidden');
            }
        }
    }

    if (statusFilter) {
        statusFilter.addEventListener('change', loadInterviews);
    }
    loadInterviews();
})();

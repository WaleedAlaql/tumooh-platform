(function () {
    const userId = window.TUMOOH_CAREER && window.TUMOOH_CAREER.userId;
    const listEl = document.getElementById('company-list');
    const errorEl = document.getElementById('career-error');
    const PLACEHOLDER_LOGO = '/images/logo-placeholder.png';

    if (!listEl || !userId) {
        return;
    }

    function escapeHtml(text) {
        return String(text == null ? '' : text)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    function renderRecommendations(recommendations) {
        listEl.innerHTML = (recommendations || []).map(function (c) {
            var logo = c.companyLogoUrl || PLACEHOLDER_LOGO;
            return '<article class="flex gap-4 rounded-2xl bg-white p-5 border border-tumooh-navy/10 shadow-soft">' +
                '<img src="' + escapeHtml(logo) + '" alt="" class="h-12 w-12 shrink-0 rounded-xl border border-tumooh-navy/10 object-contain bg-white p-1" onerror="this.src=\'' + PLACEHOLDER_LOGO + '\'"/>' +
                '<div class="min-w-0 flex-1">' +
                '<h3 class="font-semibold text-lg">' + escapeHtml(c.name) + '</h3>' +
                '<p class="text-sm text-tumooh-navy/60 mt-1">' + escapeHtml(c.industry || '') + '</p>' +
                '<p class="mt-3 text-tumooh-navy/80">' + escapeHtml(c.reason || '') + '</p>' +
                '</div></article>';
        }).join('') || '<p class="text-tumooh-navy/60">No recommendations returned.</p>';
    }

    async function loadRecommendations() {
        if (errorEl) {
            errorEl.classList.add('hidden');
        }
        listEl.innerHTML = '<p class="text-tumooh-navy/60">Loading recommendations…</p>';
        try {
            const data = await window.TumoohApi.post('/ai/users/' + userId + '/company-recommendations');
            renderRecommendations(data.recommendations);
        } catch (err) {
            listEl.innerHTML = '';
            if (errorEl) {
                errorEl.textContent = err.message;
                errorEl.classList.remove('hidden');
            }
        }
    }

    loadRecommendations();
})();

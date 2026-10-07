package org.fadhel.tumoohplatform.repository;

import org.fadhel.tumoohplatform.model.Interview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, Long> {
    List<Interview> findByUserId(Long userId);
    List<Interview> findByJobApplicationId(Long jobApplicationId);
    List<Interview> findByStatus(String status);
    List<Interview> findByUserIdAndInterviewDateGreaterThanEqualOrderByInterviewDateAsc(
            Long userId, LocalDateTime from);

    List<Interview> findByInterviewDateAfterAndStatusIgnoreCase(LocalDateTime after, String status);
}
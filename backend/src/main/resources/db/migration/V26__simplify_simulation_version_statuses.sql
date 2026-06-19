UPDATE simulation_versions
SET status = 'DRAFT'
WHERE status IN ('IN_REVIEW', 'APPROVED', 'REJECTED');

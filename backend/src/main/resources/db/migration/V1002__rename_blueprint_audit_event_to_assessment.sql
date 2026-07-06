UPDATE audit_events
SET event_type = 'simulationVersionAssessmentUpdated'
WHERE event_type = 'simulationVersionBlueprintUpdated';

import type { DashboardResponse } from "@/lib/api/praxis";

export type OnboardingStepKey =
  | "assessment"
  | "scenario"
  | "publish"
  | "journey"
  | "invite"
  | "result";

export function onboardingCompletion(
  dashboard?: DashboardResponse,
): Record<OnboardingStepKey, boolean> {
  if (!dashboard) {
    return {
      assessment: false,
      scenario: false,
      publish: false,
      journey: false,
      invite: false,
      result: false,
    };
  }

  const hasPublishedAssessment = dashboard.activeSimulations > 0;
  const hasJourney = dashboard.assessmentJourneys.total > 0;
  const hasCandidateActivity =
    dashboard.candidatesInProgress > 0 || dashboard.completedAttemptsLast30Days > 0;
  const hasResult =
    dashboard.completedAttemptsLast30Days > 0 || dashboard.latestResults.length > 0;

  return {
    assessment: hasPublishedAssessment,
    scenario: hasPublishedAssessment,
    publish: hasPublishedAssessment,
    journey: hasJourney,
    invite: hasCandidateActivity,
    result: hasResult,
  };
}

export function isOnboardingComplete(dashboard?: DashboardResponse): boolean {
  return Object.values(onboardingCompletion(dashboard)).every(Boolean);
}

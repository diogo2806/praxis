package br.com.iforce.praxis.gupy.service;

final class CandidateAttemptIdempotencyScope implements AutoCloseable {

    private static final ThreadLocal<Override> CURRENT = new ThreadLocal<>();

    private final Override previous;

    private CandidateAttemptIdempotencyScope(Override previous) {
        this.previous = previous;
    }

    static CandidateAttemptIdempotencyScope open(String rawSource, String resolvedHash) {
        Override previous = CURRENT.get();
        CURRENT.set(new Override(rawSource, resolvedHash));
        return new CandidateAttemptIdempotencyScope(previous);
    }

    static String resolve(String rawSource) {
        Override current = CURRENT.get();
        if (current == null || !current.rawSource().equals(rawSource)) {
            return null;
        }
        return current.resolvedHash();
    }

    @Override
    public void close() {
        if (previous == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(previous);
        }
    }

    private record Override(String rawSource, String resolvedHash) {
    }
}

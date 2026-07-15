package br.com.iforce.praxis.gupy.service;

final class CandidateAttemptIdempotencyScope implements AutoCloseable {

    private static final ThreadLocal<ScopeValue> CURRENT = new ThreadLocal<>();

    private final ScopeValue previous;

    private CandidateAttemptIdempotencyScope(ScopeValue previous) {
        this.previous = previous;
    }

    static CandidateAttemptIdempotencyScope open(String rawSource, String resolvedHash) {
        ScopeValue previous = CURRENT.get();
        CURRENT.set(new ScopeValue(rawSource, resolvedHash));
        return new CandidateAttemptIdempotencyScope(previous);
    }

    static String resolve(String rawSource) {
        ScopeValue current = CURRENT.get();
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

    private record ScopeValue(String rawSource, String resolvedHash) {
    }
}

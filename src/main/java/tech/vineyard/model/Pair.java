package tech.vineyard.model;

public record Pair<T>(T first, T second) {
    @Override
    public String toString() {
        return String.format("pair_%s_%s", first, second);
    }
}

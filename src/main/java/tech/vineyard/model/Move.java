package tech.vineyard.model;

public record Move(Position start, Position end) {
    @Override
    public String toString() {
        return String.format("%s %s", start, end);
    }
}

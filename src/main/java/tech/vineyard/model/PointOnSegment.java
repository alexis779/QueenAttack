package tech.vineyard.model;

public record PointOnSegment(Pair<Queen> segment, Piece point) {
    @Override
    public String toString() {
        return String.format("pointOnSegment_%s_%s", segment, point);
    }
}

package tech.vineyard.model;

public record Queen(int id) implements Piece {

    @Override
    public String toString() {
        return String.format("queen_%s", id);
    }
}

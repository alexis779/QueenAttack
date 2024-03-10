package tech.vineyard.model;

public record Wall(int id) implements Piece {

    @Override
    public String toString() {
        return String.format("wall_%s", id);
    }
}

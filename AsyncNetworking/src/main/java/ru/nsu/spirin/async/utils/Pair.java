package ru.nsu.spirin.async.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

@RequiredArgsConstructor
public final class Pair {
    private @Getter final int x;
    private @Getter final int y;

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Pair)) {
            return false;
        }
        Pair that = (Pair)object;
        return this.x == that.x && this.y == that.y;
    }
}

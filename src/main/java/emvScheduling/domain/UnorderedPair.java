package emvScheduling.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
/**
 * this implementation of pair, ignores the order and (x,y) is equal to (y,x)
 */
public class UnorderedPair<T extends Comparable<T>> implements Comparable<UnorderedPair<T>> {
    private T i;
    private T j;

    public UnorderedPair(T i, T j) {
        this.i = i;
        this.j = j;
    }

    @Override
    public String toString() {
        return "(" + i + ", " + j + ')';
    }

    // Override equals to ignore order
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnorderedPair<?> pair = (UnorderedPair<?>) o;

        // Compare both ways to ignore order
        return (Objects.equals(i, pair.i) && Objects.equals(j, pair.j)) ||
                (Objects.equals(i, pair.j) && Objects.equals(j, pair.i));
    }

    //ignore order
    @Override
    public int hashCode() {
        int hashFirst = Objects.hash(i);
        int hashSecond = Objects.hash(j);

        // Combine the hash codes using a combination of sum and product
        return hashFirst + hashSecond + 31 * hashFirst * hashSecond;
    }

    public void setValues(T i, T j) {
        this.i = i;
        this.j = j;
    }

    @Override
    public int compareTo(UnorderedPair<T> p2) {
        // First compare the first elements
        int cmp = this.getI().compareTo(p2.getI());
        if (cmp != 0) {
            return cmp;
        }
        // If first elements are equal, compare the second elements
        return this.getJ().compareTo(p2.getJ());
    }
}

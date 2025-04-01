package emvScheduling.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Computer {
    private Integer id;
    private Integer costPerOperation;
    private Integer costPerIdleTime;

    public Computer(Integer id, Integer costPerOperation, Integer costPerIdleTime) {
        this.id = id;
        this.costPerOperation = costPerOperation;
        this.costPerIdleTime = costPerIdleTime;
    }

    @Override
    public String toString() {
        return "Computer{" +
                "id=" + id +
                ", costPerOperation=" + costPerOperation +
                ", costPerIdleTime=" + costPerIdleTime +
                '}';
    }
}

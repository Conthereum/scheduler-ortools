package emvScheduling.domain;

import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.IntervalVar;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
public class Process {
    // facts:
    @NonNull
    private Integer id;
    @NonNull
    private Integer executionTime;//in millisecond
    @NonNull
    private Integer operationCount;


    //decision variables:
    private IntVar startTime;
    private IntVar computerId;

    //dependable variables:
    private IntVar endTime;
    private IntervalVar interval;

    public Process(@NonNull Integer id, @NonNull Integer executionTime, @NonNull Integer operationCount) {
        this.id = id;
        this.executionTime = executionTime;
        this.operationCount = operationCount;
    }

    @Override
    public String toString() {
        return "Process{" +
                "id=" + id +
                ", exeTime=" + executionTime +
                ", operationCount=" + operationCount +
                ", computerId=" + computerId.toString() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Process)) return false;
        Process that = (Process) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

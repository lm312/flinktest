import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple1;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;

import java.util.ArrayList;
import java.util.List;

public class OperatorStateTest {
    public  void main(String[] args) throws Exception {

        // set up the streaming execution environment
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        //默认情况下，检查点被禁用。要启用检查点，请在StreamExecutionEnvironment上调用enableCheckpointing(n)方法，
        // 其中n是以毫秒为单位的检查点间隔。每隔5000 ms进行启动一个检查点,则下一个检查点将在上一个检查点完成后5秒钟内启动
        env.enableCheckpointing(5000);
        // this can be used in a streaming program like this (assuming we have a StreamExecutionEnvironment env)
        env.fromElements(Tuple2.of(1L, 3L), Tuple2.of(1L, 5L), Tuple2.of(1L, 7L), Tuple2.of(1L, 4L), Tuple2.of(1L, 2L))
                .keyBy(0)
                .flatMap(new CountWindowAverageTest.CountWindowAverage())
                .print();
        env.fromElements(Tuple1.of(1)).addSink(new BufferingSink(3));
        env.execute("Flink Streaming Java API Skeleton");
    }
    public  class BufferingSink
            implements SinkFunction<Tuple2<String, Integer>>,
            CheckpointedFunction {

        private final int threshold;

        private transient ListState<Tuple2<String, Integer>> checkpointedState;

        private List<Tuple2<String, Integer>> bufferedElements;

        public BufferingSink(int threshold) {
            this.threshold = threshold;
            this.bufferedElements = new ArrayList<Tuple2<String, Integer>>();
        }

        @Override
        public void invoke(Tuple2<String, Integer> value) throws Exception {
            bufferedElements.add(value);
            if (bufferedElements.size() == threshold) {
                for (Tuple2<String, Integer> element: bufferedElements) {
                    // send it to the sink
                }
                bufferedElements.clear();
            }
        }

        @Override
        public void snapshotState(FunctionSnapshotContext context) throws Exception {
            checkpointedState.clear();
            for (Tuple2<String, Integer> element : bufferedElements) {
                checkpointedState.add(element);
            }
        }

        @Override
        public void initializeState(FunctionInitializationContext context) throws Exception {
            ListStateDescriptor<Tuple2<String, Integer>> descriptor =
                    new ListStateDescriptor<Tuple2<String, Integer>>(
                            "buffered-elements",
                            TypeInformation.of(new TypeHint<Tuple2<String, Integer>>() {}));

            checkpointedState = context.getOperatorStateStore().getListState(descriptor);

            if (context.isRestored()) {
                for (Tuple2<String, Integer> element : checkpointedState.get()) {
                    bufferedElements.add(element);
                }
            }
        }
    }
}

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer09;
import org.apache.flink.streaming.util.serialization.SimpleStringSchema;
import org.apache.flink.util.Collector;

import java.util.Properties;


public class KafkaDemo {

    public static void main(String[] args) throws Exception {

        // set up the streaming execution environment
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        //默认情况下，检查点被禁用。要启用检查点，请在StreamExecutionEnvironment上调用enableCheckpointing(n)方法，
        // 其中n是以毫秒为单位的检查点间隔。每隔5000 ms进行启动一个检查点,则下一个检查点将在上一个检查点完成后5秒钟内启动
        env.enableCheckpointing(5000);
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "localhost:9092");//kafka的节点的IP或者hostName，多个使用逗号分隔
        properties.setProperty("zookeeper.connect", "localhost:2181");//zookeeper的节点的IP或者hostName，多个使用逗号进行分隔
        properties.setProperty("group.id", "test-consumer-group");//flink consumer flink的消费者的group.id
        FlinkKafkaConsumer09<String> myConsumer = new FlinkKafkaConsumer09<String>("test1", new SimpleStringSchema(),
                properties);//test1是kafka中开启的topic
        myConsumer.assignTimestampsAndWatermarks(new CustomWatermarkEmitter());
       // DataStreamSource<String> keyedStream
        DataStreamSource<String> keyedStream = env.addSource(myConsumer);//将kafka生产者发来的数据进行处理，本例子我没进行任何处理
//wordcount
        DataStream<String> stream = env.addSource(myConsumer);

        DataStream<Tuple2<String, Integer>> counts = stream.flatMap(new LineSplitter()).keyBy(0).sum(1);
//wordcount end
        counts.print();
        keyedStream.print();
//计算数据
   /*     DataStream<WordWithCount> windowCount = keyedStream.flatMap(new FlatMapFunction<String, WordWithCount>() {
            public void flatMap(String value, Collector<WordWithCount> out) throws Exception {
                String[] splits = value.split("\\s");
                for (String word:splits) {
                    out.collect(new WordWithCount(word,1L));
                }
            }
        })//打平操作，把每行的单词转为<word,count>类型的数据
                .keyBy("word")//针对相同的word数据进行分组
                .timeWindow(Time.seconds(2),Time.seconds(1))//指定计算数据的窗口大小和滑动窗口大小
                .sum("count");

        //把数据打印到控制台
        windowCount.print()
                .setParallelism(1);//使用一个并行度*/
        //注意：因为flink是懒加载的，所以必须调用execute方法，上面的代码才会执行
        env.execute("streaming word count");


/*
        DataSet<Tuple2<String, Integer>> counts = keyedStream.flatMap(new Tokenizer()).groupBy(0).sum(1);

        counts.writeAsCsv(outPath, "\n", " ").setParallelism(1);
        keyedStream.print();//直接将从生产者接收到的数据在控制台上进行打印
        // execute program
        env.execute("Flink Streaming Java API Skeleton");
*/
    }
    public static final class LineSplitter implements FlatMapFunction<String, Tuple2<String, Integer>> {
        private static final long serialVersionUID = 1L;

        public void flatMap(String value, Collector<Tuple2<String, Integer>> out) {
            String[] tokens = value.toLowerCase().split("\\W+");
            for (String token : tokens) {
                if (token.length() > 0) {
                    out.collect(new Tuple2<String, Integer>(token, 1));
                }
            }
        }
    }
    /**
     * 主要为了存储单词以及单词出现的次数
     */
    public static class WordWithCount{
        public String word;
        public long count;
        public WordWithCount(){}
        public WordWithCount(String word, long count) {
            this.word = word;
            this.count = count;
        }

        @Override
        public String toString() {
            return "WordWithCount{" +
                    "word='" + word + '\'' +
                    ", count=" + count +
                    '}';
        }
    }
    /*
    public static class Tokenizer implements FlatMapFunction<String, Tuple2<String,Integer>> {
        public void flatMap(String value, Collector<Tuple2<String, Integer>> out) throws Exception {
            String[] tokens = value.toLowerCase().split("\\W+");
            for (String token : tokens
            ) {
                if (token.length() > 0) {
                    out.collect(new Tuple2<String, Integer>(token, 1));
                }
            }
        }
    }
    */
}
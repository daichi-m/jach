package io.github.daichim.jach.cucumber;

/*
@Slf4j
public class ChannelSetupStepDefs implements En {

    public ExecutorService threadPool;
    private BufferedChannel<Integer> channel;
    private MessageValidator generator;
    private Collection<Runnable> writers;
    private Collection<Runnable> readers;

    public ChannelSetupStepDefs(MessageValidator generator) {
        this.threadPool = Executors.newCachedThreadPool(new ThreadFactoryBuilder()
            .setNameFormat("cucumber-runner-%d").build());
        this.writers = new ArrayList<>();
        this.readers = new ArrayList<>();

        Given("A channel of size (\\d+)", (Integer cap) -> {
            channel = new BufferedChannel<>(cap);
        });
        Given("(\\d+) writers", (Integer wrt) -> {
            this.writers = IntStream.range(0, wrt)
                .mapToObj(i -> createWriter())
                .collect(Collectors.toList());
        });
        Consumer<Integer> consumer = i -> {
            generator.verify(i);
        };
        Given("(\\d+) readers", (Integer wrt) -> {
            this.readers = IntStream.range(0, wrt)
                .mapToObj(i -> createReader(consumer))
                .collect(Collectors.toList());
        });

        When("(\\d+) messages are written", (Integer msgCt) -> {
            for (int i=0;i <msgCt; i++) {
                int msg = generator.newMessage();

            }
            this.threadPool.invokeAll(writers)
        });

    }

    private Runnable createWriter() {
        return () -> {
            channel.write(generator.newMessage());
        };
    }

    private Runnable createReader(Consumer<Integer> action) {

        return () -> {
            Integer msg = channel.read();
            action.accept(msg);
        };
    }

} */
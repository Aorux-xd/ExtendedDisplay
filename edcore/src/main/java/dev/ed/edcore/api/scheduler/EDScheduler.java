package dev.ed.edcore.api.scheduler;

/**
 * Планировщик задач (синхронно с игровым циклом / асинхронно / с задержкой).
 * Задержки задаются в миллисекундах; на Paper для синхронных задач используется тик ~50 мс.
 */
public interface EDScheduler {

    void runSync(Runnable task);

    void runAsync(Runnable task);

    void runLater(long delayMillis, Runnable task);
}

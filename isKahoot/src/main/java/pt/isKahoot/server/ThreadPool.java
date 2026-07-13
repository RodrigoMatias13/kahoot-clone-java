package pt.isKahoot.server;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Implementação simples de uma thread pool.
 *
 * Permite:
 *  - limitar o número de threads ativas
 *  - enfileirar tarefas
 *  - reutilizar capacidade de execução
 */

public class ThreadPool {
    private final int maxThreads;
    private int activeThreads = 0;
    private final Queue<Runnable> taskQueue = new LinkedList<>();
    private boolean isShutdown = false;

    public ThreadPool(int maxThreads) {
        this.maxThreads = maxThreads;
    }

 // Adiciona nova tarefa à fila de execução
    public synchronized void submit(Runnable task) {
        if (isShutdown) throw new IllegalStateException("ThreadPool encerrada.");
        taskQueue.add(task);
        dispatch();
    }

    
    /*
     * Lança novas threads enquanto existir:
     *  - capacidade disponível
     *  - tarefas pendentes
     */
    private void dispatch() {
        while (activeThreads < maxThreads && !taskQueue.isEmpty()) {
            Runnable nextTask = taskQueue.poll();
            activeThreads++;
            Thread worker = new Thread(() -> {
                try {
                    nextTask.run();
                } finally {
                    synchronized (ThreadPool.this) {
                        activeThreads--;
                        dispatch();
                    }
                }
            });
            worker.setDaemon(true);
            worker.start();
        }
    }

    public synchronized void shutdown() { this.isShutdown = true; }
}
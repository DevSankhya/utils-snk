package br.com.sankhya.ce.concurrent;

import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.modelcore.util.AsyncAction;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class Async {
    /**
     * Lista de tarefas registradas.
     */
    List<Task> tasks = new ArrayList<>();


    /**
     * Comparator utilizado para ordenar tarefas por ordem de execução.
     */
    private final Comparator<Task> comparator = Comparator.comparingInt(Task::getOrder);


    /**
     * Cria uma nova instância utilizando um array de tarefas.
     *
     * <p>
     * As tarefas são automaticamente ordenadas pelo campo {@code order}.
     * </p>
     *
     * @param tasks Array de tarefas.
     */
    public Async(@NotNull Task... tasks) {
        this.tasks = Arrays.stream(tasks).sorted(comparator).collect(Collectors.toList());
    }

    /**
     * Cria uma nova instância utilizando uma lista de tarefas.
     *
     * <p>
     * As tarefas são automaticamente ordenadas pelo campo {@code order}.
     * </p>
     *
     * @param tasks Lista de tarefas.
     */
    public Async(@NotNull Collection<Task> tasks) {
        this.tasks = tasks.stream().sorted(comparator).collect(Collectors.toList());
    }

    /**
     * Cria uma instância vazia.
     *
     * <p>
     * Novas tarefas podem ser adicionadas posteriormente através de
     * {@link #addTask(Task)}.
     * </p>
     */
    public Async() {

    }

    /**
     * Adiciona uma nova tarefa à fila de execução.
     *
     * @param task Tarefa a ser adicionada.
     */
    public void addTask(Task task) {
        tasks.add(task);
    }

    /**
     * Executa todas as tarefas assincronamente.
     *
     * <p>
     * As tarefas são executadas em paralelo utilizando um pool fixo
     * de 5 threads.
     * </p>
     *
     * <p>
     * Este método bloqueia até que todas as tarefas terminem.
     * </p>
     */
    public void run() {
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Task task : tasks) {
            String description = task.getDescription();
            if (description != null)
                System.out.println(description);

            CompletableFuture<Void> future = CompletableFuture.runAsync(getRunnable(task), executorService);
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).
            join();
        executorService.shutdown();
    }

    /**
     * Cria um {@link Runnable} seguro para execução da tarefa.
     *
     * <p>
     * Qualquer exceção lançada pela tarefa será encapsulada em
     * {@link RuntimeException}.
     * </p>
     *
     * @param task Tarefa a ser executada.
     * @return Runnable configurado.
     */
    private static @NotNull Runnable getRunnable(Task task) {
        return () -> {
            try {
                if (task != null)
                    task.action();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    /**
     * Executa todas as tarefas de forma síncrona.
     *
     * <p>
     * As tarefas são executadas sequencialmente na thread atual.
     * </p>
     *
     * @throws Exception Caso alguma tarefa falhe.
     */
    public void runSync() throws Exception {
        for (Task task : tasks) {
            String description = task.getDescription();
            if (description != null)
                System.out.println(description);
            task.action();
        }
    }

    /**
     * Executa tarefas em lotes (chunks).
     *
     * <p>
     * Cada chunk é executado em paralelo utilizando um pool fixo
     * de 5 threads.
     * </p>
     *
     * <p>
     * O próximo chunk só será executado após a conclusão do anterior.
     * </p>
     *
     * @param chunkSize Quantidade máxima de tarefas por chunk.
     */
    public void runChunked(int chunkSize) {
        List<List<Task>> chunks = generateChunks(tasks, chunkSize);
        for (List<Task> chunk : chunks) {
            ExecutorService executorService = Executors.newFixedThreadPool(5);
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (Task task : chunk) {
                String description = task.getDescription();

                if (description != null && !description.isEmpty()) {
                    System.out.println("Description: " + description);
                }
                CompletableFuture<Void> future = CompletableFuture.runAsync(getRunnable(task));
                futures.add(future);
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).
                join();
            executorService.shutdown();
        }
    }


    /**
     * Executa tarefas em chunks utilizando tamanho padrão igual a 5.
     *
     * <p>
     * O comportamento é equivalente a:
     * </p>
     *
     * <pre>
     * runChunked(5)
     * </pre>
     */
    public void runChunked() {
        List<List<Task>> chunks = generateChunks(tasks, 5);
        for (List<Task> chunk : chunks) {
            ExecutorService executorService = Executors.newFixedThreadPool(5);
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (Task task : chunk) {
                String description = task.getDescription();

                if (description != null && !description.isEmpty()) {
                    System.out.println("Description: " + description);
                }
                CompletableFuture<Void> future = CompletableFuture.runAsync(getRunnable(task));
                futures.add(future);
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).
                join();
            executorService.shutdown();
        }
    }

    /**
     * Divide uma lista de tarefas em múltiplos chunks.
     *
     * @param tasks     Lista original de tarefas.
     * @param chunkSize Tamanho máximo do chunk.
     * @return Lista contendo sublistas de tarefas.
     */
    private List<List<Task>> generateChunks(List<Task> tasks, int chunkSize) {
        List<List<Task>> chunks = new ArrayList<>();
        int i = 0;
        while (i < tasks.size()) {
            chunks.add(tasks.subList(i, Math.min(i + chunkSize, tasks.size())));
            i += chunkSize;
        }
        return chunks;
    }


    /**
     * Executa uma tarefa utilizando o sistema assíncrono do Sankhya.
     *
     * <p>
     * Este método:
     * </p>
     *
     * <ul>
     *     <li>Cria uma {@link AsyncAction.AsyncTask}</li>
     *     <li>Abre automaticamente sessão {@link JapeSession}</li>
     *     <li>Garante execução transacional com {@code execEnsuringTX()}</li>
     *     <li>Adiciona a tarefa à fila do {@link AsyncAction}</li>
     * </ul>
     *
     * <p>
     * Ideal para processamento assíncrono desacoplado da requisição atual.
     * </p>
     *
     * @param name   Nome descritivo da tarefa.
     * @param domain Domínio/grupo da tarefa.
     * @param body   Código que será executado assincronamente.
     */
    public static void async(
        String name, // Nome da tasnk
        String domain, // Dominio da task(Pode ser qualquer coisa at� onde sei, mas � para ser um string sem espa�os como um ID. Syspdv usa "dvc" por exemplo)
        Runnable body // C�digo que vai ser executado no AsyncAction
    ) {
        AsyncAction.AsyncTask task = new AsyncAction.AsyncTask(name, true, true);
        task.setTaskBody(() -> {
            JapeSession.SessionHandle hnd = null;
            try {
                // Abre uma sess�o(N�o significa que vai abrir uma transa��o
                hnd = JapeSession.open();
                // Garante que vai ter transa��o ativa(Abre uma nova se n�o existir ou usa a correte se existir)
                hnd.execEnsuringTX(() -> {
                    body.run();
                });
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (hnd != null)
                    JapeSession.close(hnd);
            }
        });
        try {
            AsyncAction.addTask(domain, task); // Adiciona a task a fila de execu��o
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

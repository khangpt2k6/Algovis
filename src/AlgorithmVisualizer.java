import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class AlgorithmVisualizer extends Application {
    private static final int CANVAS_WIDTH = 800;
    private static final int CANVAS_HEIGHT = 400;
    private static final int MAX_ARRAY_SIZE = 100;
    private static final int MIN_DELAY = 1;
    private static final int MAX_DELAY = 500;
    
    private Canvas canvas;
    private GraphicsContext gc;
    private int[] array;
    private int arraySize = 50;
    private int delay = 50;
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private AtomicBoolean isPaused = new AtomicBoolean(false);
    
    // UI Controls
    private ComboBox<String> algorithmComboBox;
    private Slider arraySizeSlider;
    private Slider speedSlider;
    private Button startButton;
    private Button pauseButton;
    private Button resetButton;
    private Button shuffleButton;
    private Label statusLabel;
    private Label comparisonsLabel;
    private Label swapsLabel;
    private ProgressBar progressBar;
    
    // Algorithm statistics
    private volatile int comparisons = 0;
    private volatile int swaps = 0;
    private volatile int currentStep = 0;
    private volatile int totalSteps = 0;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Java Algorithm Visualizer");
        
        initializeArray();
        setupUI(primaryStage);
        
        primaryStage.show();
        drawArray();
    }

    private void setupUI(Stage primaryStage) {
        BorderPane root = new BorderPane();
        
        // Create canvas
        canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        gc = canvas.getGraphicsContext2D();
        
        // Control panel
        VBox controlPanel = createControlPanel();
        
        // Status panel
        HBox statusPanel = createStatusPanel();
        
        root.setCenter(canvas);
        root.setTop(controlPanel);
        root.setBottom(statusPanel);
        
        Scene scene = new Scene(root, CANVAS_WIDTH + 50, CANVAS_HEIGHT + 150);
        scene.getStylesheets().add("data:text/css," + getCSS());
        primaryStage.setScene(scene);
    }

    private VBox createControlPanel() {
        VBox controlPanel = new VBox(10);
        controlPanel.setPadding(new Insets(10));
        controlPanel.setAlignment(Pos.CENTER);
        controlPanel.getStyleClass().add("control-panel");
        
        // Algorithm selection
        HBox algorithmBox = new HBox(10);
        algorithmBox.setAlignment(Pos.CENTER);
        Label algorithmLabel = new Label("Algorithm:");
        algorithmComboBox = new ComboBox<>();
        algorithmComboBox.getItems().addAll(
            "Bubble Sort", "Selection Sort", "Insertion Sort", 
            "Merge Sort", "Quick Sort", "Heap Sort"
        );
        algorithmComboBox.setValue("Bubble Sort");
        algorithmBox.getChildren().addAll(algorithmLabel, algorithmComboBox);
        
        // Array size control
        HBox sizeBox = new HBox(10);
        sizeBox.setAlignment(Pos.CENTER);
        Label sizeLabel = new Label("Array Size:");
        arraySizeSlider = new Slider(10, MAX_ARRAY_SIZE, arraySize);
        arraySizeSlider.setShowTickMarks(true);
        arraySizeSlider.setShowTickLabels(true);
        arraySizeSlider.setMajorTickUnit(20);
        Label sizeValueLabel = new Label(String.valueOf(arraySize));
        arraySizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            arraySize = newVal.intValue();
            sizeValueLabel.setText(String.valueOf(arraySize));
            if (!isRunning.get()) {
                initializeArray();
                drawArray();
                resetStats();
            }
        });
        sizeBox.getChildren().addAll(sizeLabel, arraySizeSlider, sizeValueLabel);
        
        // Speed control
        HBox speedBox = new HBox(10);
        speedBox.setAlignment(Pos.CENTER);
        Label speedLabel = new Label("Speed:");
        speedSlider = new Slider(MIN_DELAY, MAX_DELAY, delay);
        speedSlider.setShowTickMarks(true);
        speedSlider.setShowTickLabels(true);
        speedSlider.setMajorTickUnit(100);
        Label speedValueLabel = new Label(delay + "ms");
        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            delay = MAX_DELAY + MIN_DELAY - newVal.intValue(); // Invert for intuitive control
            speedValueLabel.setText(delay + "ms");
        });
        speedBox.getChildren().addAll(speedLabel, speedSlider, speedValueLabel);
        
        // Control buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        
        startButton = new Button("Start");
        startButton.getStyleClass().add("start-button");
        startButton.setOnAction(e -> startSorting());
        
        pauseButton = new Button("Pause");
        pauseButton.getStyleClass().add("pause-button");
        pauseButton.setDisable(true);
        pauseButton.setOnAction(e -> togglePause());
        
        resetButton = new Button("Reset");
        resetButton.getStyleClass().add("reset-button");
        resetButton.setOnAction(e -> resetArray());
        
        shuffleButton = new Button("Shuffle");
        shuffleButton.getStyleClass().add("shuffle-button");
        shuffleButton.setOnAction(e -> shuffleArray());
        
        buttonBox.getChildren().addAll(startButton, pauseButton, resetButton, shuffleButton);
        
        controlPanel.getChildren().addAll(algorithmBox, sizeBox, speedBox, buttonBox);
        return controlPanel;
    }

    private HBox createStatusPanel() {
        HBox statusPanel = new HBox(20);
        statusPanel.setPadding(new Insets(10));
        statusPanel.setAlignment(Pos.CENTER);
        statusPanel.getStyleClass().add("status-panel");
        
        statusLabel = new Label("Ready");
        statusLabel.getStyleClass().add("status-label");
        
        comparisonsLabel = new Label("Comparisons: 0");
        comparisonsLabel.getStyleClass().add("stat-label");
        
        swapsLabel = new Label("Swaps: 0");
        swapsLabel.getStyleClass().add("stat-label");
        
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(200);
        
        statusPanel.getChildren().addAll(statusLabel, comparisonsLabel, swapsLabel, progressBar);
        return statusPanel;
    }

    private void initializeArray() {
        array = new int[arraySize];
        for (int i = 0; i < arraySize; i++) {
            array[i] = i + 1;
        }
        shuffleArray();
    }

    private void shuffleArray() {
        if (isRunning.get()) return;
        
        List<Integer> list = new ArrayList<>(Arrays.stream(array).boxed().toList());
        Collections.shuffle(list);
        array = list.stream().mapToInt(Integer::intValue).toArray();
        drawArray();
        resetStats();
    }

    private void resetArray() {
        if (isRunning.get()) {
            isRunning.set(false);
            isPaused.set(false);
        }
        
        initializeArray();
        drawArray();
        resetStats();
        updateButtonStates();
        updateStatus("Ready");
    }

    private void resetStats() {
        comparisons = 0;
        swaps = 0;
        currentStep = 0;
        totalSteps = 0;
        Platform.runLater(() -> {
            comparisonsLabel.setText("Comparisons: 0");
            swapsLabel.setText("Swaps: 0");
            progressBar.setProgress(0);
        });
    }

    private void startSorting() {
        if (isRunning.get()) return;
        
        isRunning.set(true);
        isPaused.set(false);
        resetStats();
        updateButtonStates();
        
        String algorithm = algorithmComboBox.getValue();
        updateStatus("Running " + algorithm + "...");
        
        CompletableFuture.runAsync(() -> {
            try {
                switch (algorithm) {
                    case "Bubble Sort" -> bubbleSort();
                    case "Selection Sort" -> selectionSort();
                    case "Insertion Sort" -> insertionSort();
                    case "Merge Sort" -> mergeSort(0, array.length - 1);
                    case "Quick Sort" -> quickSort(0, array.length - 1);
                    case "Heap Sort" -> heapSort();
                }
                
                if (isRunning.get()) {
                    Platform.runLater(() -> {
                        updateStatus("Sorting completed!");
                        highlightSortedArray();
                    });
                }
            } catch (InterruptedException e) {
                Platform.runLater(() -> updateStatus("Sorting interrupted"));
            } finally {
                isRunning.set(false);
                isPaused.set(false);
                Platform.runLater(this::updateButtonStates);
            }
        });
    }

    private void togglePause() {
        isPaused.set(!isPaused.get());
        Platform.runLater(() -> {
            pauseButton.setText(isPaused.get() ? "Resume" : "Pause");
            updateStatus(isPaused.get() ? "Paused" : "Running " + algorithmComboBox.getValue() + "...");
        });
    }

    private void updateButtonStates() {
        boolean running = isRunning.get();
        startButton.setDisable(running);
        pauseButton.setDisable(!running);
        resetButton.setDisable(false);
        shuffleButton.setDisable(running);
        algorithmComboBox.setDisable(running);
        arraySizeSlider.setDisable(running);
    }

    private void updateStatus(String status) {
        Platform.runLater(() -> statusLabel.setText(status));
    }

    private void updateStats() {
        Platform.runLater(() -> {
            comparisonsLabel.setText("Comparisons: " + comparisons);
            swapsLabel.setText("Swaps: " + swaps);
            if (totalSteps > 0) {
                progressBar.setProgress((double) currentStep / totalSteps);
            }
        });
    }

    private void waitForDelay() throws InterruptedException {
        Thread.sleep(delay);
        while (isPaused.get() && isRunning.get()) {
            Thread.sleep(50);
        }
        if (!isRunning.get()) {
            throw new InterruptedException("Sorting stopped");
        }
    }

    private void drawArray() {
        drawArray(-1, -1, Color.LIGHTBLUE);
    }

    private void drawArray(int highlightIndex1, int highlightIndex2, Color highlightColor) {
        Platform.runLater(() -> {
            gc.clearRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
            
            if (array == null || array.length == 0) return;
            
            double barWidth = (double) CANVAS_WIDTH / array.length;
            double maxHeight = CANVAS_HEIGHT - 50;
            int maxValue = Arrays.stream(array).max().orElse(1);
            
            for (int i = 0; i < array.length; i++) {
                double barHeight = (double) array[i] / maxValue * maxHeight;
                double x = i * barWidth;
                double y = CANVAS_HEIGHT - barHeight;
                
                // Set color based on highlighting
                if (i == highlightIndex1 || i == highlightIndex2) {
                    gc.setFill(highlightColor);
                } else {
                    gc.setFill(Color.LIGHTBLUE);
                }
                
                gc.fillRect(x, y, barWidth - 1, barHeight);
                
                // Draw border
                gc.setStroke(Color.DARKBLUE);
                gc.strokeRect(x, y, barWidth - 1, barHeight);
                
                // Draw value on top of bar if array is small enough
                if (array.length <= 20) {
                    gc.setFill(Color.BLACK);
                    gc.setFont(Font.font("Arial", FontWeight.BOLD, 12));
                    gc.fillText(String.valueOf(array[i]), x + barWidth/2 - 5, y - 5);
                }
            }
        });
    }

    private void highlightSortedArray() {
        CompletableFuture.runAsync(() -> {
            try {
                for (int i = 0; i < array.length && isRunning.get(); i++) {
                    drawArray(i, -1, Color.LIGHTGREEN);
                    Thread.sleep(20);
                }
                drawArray(-1, -1, Color.LIGHTGREEN);
            } catch (InterruptedException e) {
                // Animation interrupted
            }
        });
    }

    // Sorting Algorithms

    private void bubbleSort() throws InterruptedException {
        totalSteps = arraySize * arraySize;
        currentStep = 0;
        
        for (int i = 0; i < array.length - 1 && isRunning.get(); i++) {
            for (int j = 0; j < array.length - i - 1 && isRunning.get(); j++) {
                comparisons++;
                currentStep++;
                
                drawArray(j, j + 1, Color.RED);
                updateStats();
                waitForDelay();
                
                if (array[j] > array[j + 1]) {
                    swap(j, j + 1);
                    swaps++;
                    drawArray(j, j + 1, Color.ORANGE);
                    updateStats();
                    waitForDelay();
                }
            }
        }
    }

    private void selectionSort() throws InterruptedException {
        totalSteps = arraySize * arraySize;
        currentStep = 0;
        
        for (int i = 0; i < array.length - 1 && isRunning.get(); i++) {
            int minIdx = i;
            drawArray(i, -1, Color.GREEN);
            
            for (int j = i + 1; j < array.length && isRunning.get(); j++) {
                comparisons++;
                currentStep++;
                
                drawArray(minIdx, j, Color.RED);
                updateStats();
                waitForDelay();
                
                if (array[j] < array[minIdx]) {
                    minIdx = j;
                }
            }
            
            if (minIdx != i) {
                swap(i, minIdx);
                swaps++;
                drawArray(i, minIdx, Color.ORANGE);
                updateStats();
                waitForDelay();
            }
        }
    }

    private void insertionSort() throws InterruptedException {
        totalSteps = arraySize * arraySize;
        currentStep = 0;
        
        for (int i = 1; i < array.length && isRunning.get(); i++) {
            int key = array[i];
            int j = i - 1;
            
            drawArray(i, -1, Color.YELLOW);
            updateStats();
            waitForDelay();
            
            while (j >= 0 && array[j] > key && isRunning.get()) {
                comparisons++;
                currentStep++;
                
                drawArray(j, j + 1, Color.RED);
                updateStats();
                waitForDelay();
                
                array[j + 1] = array[j];
                swaps++;
                j--;
                
                drawArray(j + 1, j + 2, Color.ORANGE);
                updateStats();
                waitForDelay();
            }
            array[j + 1] = key;
        }
    }

    private void mergeSort(int left, int right) throws InterruptedException {
        if (left < right && isRunning.get()) {
            int mid = left + (right - left) / 2;
            
            mergeSort(left, mid);
            mergeSort(mid + 1, right);
            merge(left, mid, right);
        }
    }

    private void merge(int left, int mid, int right) throws InterruptedException {
        int[] leftArray = Arrays.copyOfRange(array, left, mid + 1);
        int[] rightArray = Arrays.copyOfRange(array, mid + 1, right + 1);
        
        int i = 0, j = 0, k = left;
        
        while (i < leftArray.length && j < rightArray.length && isRunning.get()) {
            comparisons++;
            
            drawArray(k, -1, Color.RED);
            updateStats();
            waitForDelay();
            
            if (leftArray[i] <= rightArray[j]) {
                array[k] = leftArray[i];
                i++;
            } else {
                array[k] = rightArray[j];
                j++;
            }
            swaps++;
            k++;
            
            drawArray(k - 1, -1, Color.ORANGE);
            updateStats();
            waitForDelay();
        }
        
        while (i < leftArray.length && isRunning.get()) {
            array[k] = leftArray[i];
            i++;
            k++;
            swaps++;
        }
        
        while (j < rightArray.length && isRunning.get()) {
            array[k] = rightArray[j];
            j++;
            k++;
            swaps++;
        }
    }

    private void quickSort(int low, int high) throws InterruptedException {
        if (low < high && isRunning.get()) {
            int pi = partition(low, high);
            quickSort(low, pi - 1);
            quickSort(pi + 1, high);
        }
    }

    private int partition(int low, int high) throws InterruptedException {
        int pivot = array[high];
        int i = low - 1;
        
        drawArray(high, -1, Color.PURPLE);
        updateStats();
        waitForDelay();
        
        for (int j = low; j < high && isRunning.get(); j++) {
            comparisons++;
            
            drawArray(j, high, Color.RED);
            updateStats();
            waitForDelay();
            
            if (array[j] < pivot) {
                i++;
                swap(i, j);
                swaps++;
                drawArray(i, j, Color.ORANGE);
                updateStats();
                waitForDelay();
            }
        }
        
        swap(i + 1, high);
        swaps++;
        drawArray(i + 1, high, Color.GREEN);
        updateStats();
        waitForDelay();
        
        return i + 1;
    }

    private void heapSort() throws InterruptedException {
        totalSteps = arraySize * arraySize;
        
        // Build heap
        for (int i = array.length / 2 - 1; i >= 0 && isRunning.get(); i--) {
            heapify(array.length, i);
        }
        
        // Extract elements from heap one by one
        for (int i = array.length - 1; i > 0 && isRunning.get(); i--) {
            swap(0, i);
            swaps++;
            drawArray(0, i, Color.ORANGE);
            updateStats();
            waitForDelay();
            
            heapify(i, 0);
        }
    }

    private void heapify(int n, int i) throws InterruptedException {
        int largest = i;
        int left = 2 * i + 1;
        int right = 2 * i + 2;
        
        if (left < n) {
            comparisons++;
            if (array[left] > array[largest]) {
                largest = left;
            }
        }
        
        if (right < n) {
            comparisons++;
            if (array[right] > array[largest]) {
                largest = right;
            }
        }
        
        if (largest != i && isRunning.get()) {
            swap(i, largest);
            swaps++;
            drawArray(i, largest, Color.RED);
            updateStats();
            waitForDelay();
            
            heapify(n, largest);
        }
    }

    private void swap(int i, int j) {
        int temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }

    private String getCSS() {
        return """
            .control-panel {
                -fx-background-color: #f0f0f0;
                -fx-border-color: #cccccc;
                -fx-border-width: 1px;
                -fx-border-radius: 5px;
                -fx-background-radius: 5px;
            }
            
            .status-panel {
                -fx-background-color: #e8e8e8;
                -fx-border-color: #cccccc;
                -fx-border-width: 1px 0 0 0;
            }
            
            .start-button {
                -fx-background-color: #4CAF50;
                -fx-text-fill: white;
                -fx-font-weight: bold;
                -fx-padding: 8 16 8 16;
                -fx-border-radius: 4px;
                -fx-background-radius: 4px;
            }
            
            .pause-button {
                -fx-background-color: #FF9800;
                -fx-text-fill: white;
                -fx-font-weight: bold;
                -fx-padding: 8 16 8 16;
                -fx-border-radius: 4px;
                -fx-background-radius: 4px;
            }
            
            .reset-button {
                -fx-background-color: #f44336;
                -fx-text-fill: white;
                -fx-font-weight: bold;
                -fx-padding: 8 16 8 16;
                -fx-border-radius: 4px;
                -fx-background-radius: 4px;
            }
            
            .shuffle-button {
                -fx-background-color: #2196F3;
                -fx-text-fill: white;
                -fx-font-weight: bold;
                -fx-padding: 8 16 8 16;
                -fx-border-radius: 4px;
                -fx-background-radius: 4px;
            }
            
            .status-label {
                -fx-font-weight: bold;
                -fx-font-size: 14px;
            }
            
            .stat-label {
                -fx-font-size: 12px;
                -fx-text-fill: #333333;
            }
            """;
    }
}

/* 
 * Additional files needed for a complete JavaFX project:
 * 
 * 1. module-info.java (if using modules):
 * 
 * module algorithmvisualizer {
 *     requires javafx.controls;
 *     requires javafx.fxml;
 *     requires java.desktop;
 *     
 *     exports your.package.name;
 * }
 * 
 * 2. Maven pom.xml dependencies:
 * 
 * <dependency>
 *     <groupId>org.openjfx</groupId>
 *     <artifactId>javafx-controls</artifactId>
 *     <version>17.0.2</version>
 * </dependency>
 * <dependency>
 *     <groupId>org.openjfx</groupId>
 *     <artifactId>javafx-fxml</artifactId>
 *     <version>17.0.2</version>
 * </dependency>
 * 
 * 3. To run: java --module-path /path/to/javafx/lib --add-modules javafx.controls,javafx.fxml Main
 */

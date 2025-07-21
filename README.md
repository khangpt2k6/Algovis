# Java Algorithm Visualizer

![Visualizer](https://github.com/user-attachments/assets/851bc6bb-57f3-49a0-9e50-e57a166a25ca)

A comprehensive **JavaFX application** that provides real-time visualization of popular sorting algorithms.  
Built to demonstrate **data structures**, **algorithms**, **multithreading**, and **UI design** skills.

---

## Features

- **6 Sorting Algorithms**:  
  Bubble Sort, Selection Sort, Insertion Sort, Merge Sort, Quick Sort, Heap Sort

- **Interactive Controls**:  
  Adjustable array size (10–100 elements) and animation speed

- **Real-time Statistics**:  
  Live tracking of comparisons, swaps, and sorting progress

- **Smooth Animations**:  
  Color-coded bars with pause/resume functionality

- **Multithreaded Design**:  
  Non-blocking UI with proper thread synchronization

---

## Technical Highlights

- **JavaFX Canvas**:  
  Custom drawing with dynamic bar charts

- **Concurrent Programming**:  
  `CompletableFuture`, `AtomicBoolean` used for thread-safe animations

- **Observer Pattern**:  
  Real-time UI updates using event listeners

- **Clean Architecture**:  
  Modular design with clear separation of concerns (MVC pattern)

---

## Requirements

- Java 11 or higher  
- JavaFX SDK 17.0.2 or higher

---

## Setup Instructions

### Option 1: Using JavaFX SDK

1. Download JavaFX SDK from [openjfx.io](https://openjfx.io)
2. Extract it to a directory  
   e.g., `C:\javafx-sdk-17.0.2`

---

## Running the Application

### Command Line (Windows)

```bash
# Compile
javac --module-path "C:\javafx-sdk-17.0.2\lib" --add-modules javafx.controls *.java

# Run
java --module-path "C:\javafx-sdk-17.0.2\lib" --add-modules javafx.controls --enable-native-access=javafx.graphics Main

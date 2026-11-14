
from fastapi import FastAPI
from pydantic import BaseModel, Field
from pathlib import Path
from typing import Optional
import csv
import threading

# ==========================================
# RANSOMGUARD PYTHON EVALUATION API
# ==========================================

app = FastAPI(
    title="RansomGuard Evaluation API",
    description="Android ransomware detection experiment results",
    version="1.0"
)

# Project directories
BASE_DIR = Path(__file__).resolve().parent.parent
DATA_DIR = BASE_DIR / "data"
RESULTS_FILE = DATA_DIR / "results.csv"

DATA_DIR.mkdir(parents=True, exist_ok=True)

CSV_LOCK = threading.Lock()

CSV_FIELDS = [
    "workload",
    "expected",
    "detected",
    "detection_delay_ms",
    "files_changed_before_alert"
]


# ==========================================
# EXPERIMENT DATA MODEL
# ==========================================

class Experiment(BaseModel):

    workload: str = Field(min_length=1)

    expected: str

    detected: bool

    detection_delay_ms: Optional[float] = None

    files_changed_before_alert: Optional[int] = None


# ==========================================
# HEALTH CHECK
# ==========================================

@app.get("/")
def home():

    return {
        "application": "RansomGuard",
        "message": "Evaluation API is running",
        "status": "online"
    }


@app.get("/health")
def health():

    return {
        "status": "online"
    }


# ==========================================
# SAVE EXPERIMENT RESULT
# ==========================================

@app.post("/experiment")
def save_experiment(item: Experiment):

    if item.expected not in ["attack", "benign"]:
        return {
            "saved": False,
            "error": "Expected must be attack or benign"
        }

    if (
        item.detection_delay_ms is not None
        and item.detection_delay_ms < 0
    ):
        return {
            "saved": False,
            "error": "Detection delay cannot be negative"
        }

    if (
        item.files_changed_before_alert is not None
        and item.files_changed_before_alert < 0
    ):
        return {
            "saved": False,
            "error": "Changed file count cannot be negative"
        }

    record = {
        "workload": item.workload,
        "expected": item.expected,
        "detected": int(item.detected),
        "detection_delay_ms": item.detection_delay_ms,
        "files_changed_before_alert":
            item.files_changed_before_alert
    }

    with CSV_LOCK:

        file_exists = RESULTS_FILE.exists()

        with open(
            RESULTS_FILE,
            "a",
            newline="",
            encoding="utf-8"
        ) as file:

            writer = csv.DictWriter(
                file,
                fieldnames=CSV_FIELDS
            )

            if not file_exists or RESULTS_FILE.stat().st_size == 0:
                writer.writeheader()

            writer.writerow(record)

    return {
        "saved": True,
        "message": "Experiment result saved",
        "workload": item.workload
    }


# ==========================================
# READ ALL EXPERIMENT RESULTS
# ==========================================

def read_results():

    if not RESULTS_FILE.exists():
        return []

    with CSV_LOCK:

        with open(
            RESULTS_FILE,
            "r",
            newline="",
            encoding="utf-8"
        ) as file:

            return list(csv.DictReader(file))


# ==========================================
# CALCULATE PRECISION AND RECALL
# ==========================================

@app.get("/results")
def get_results():

    rows = read_results()

    true_positive = 0
    false_positive = 0
    true_negative = 0
    false_negative = 0

    for row in rows:

        expected_attack = (
            row["expected"] == "attack"
        )

        detected_attack = (
            row["detected"] == "1"
        )

        if expected_attack and detected_attack:

            true_positive += 1

        elif not expected_attack and detected_attack:

            false_positive += 1

        elif not expected_attack and not detected_attack:

            true_negative += 1

        elif expected_attack and not detected_attack:

            false_negative += 1

    # Precision = TP / (TP + FP)
    precision = (
        true_positive /
        (true_positive + false_positive) * 100
        if true_positive + false_positive > 0
        else 0
    )

    # Recall = TP / (TP + FN)
    recall = (
        true_positive /
        (true_positive + false_negative) * 100
        if true_positive + false_negative > 0
        else 0
    )

    # Accuracy
    accuracy = (
        (true_positive + true_negative) /
        len(rows) * 100
        if len(rows) > 0
        else 0
    )

    return {
        "total": len(rows),

        "tp": true_positive,
        "fp": false_positive,
        "tn": true_negative,
        "fn": false_negative,

        "precision": round(precision, 2),
        "recall": round(recall, 2),
        "accuracy": round(accuracy, 2)
    }


# ==========================================
# CLEAR EXPERIMENT RESULTS
# ==========================================

@app.delete("/results")
def clear_results():

    with CSV_LOCK:

        if RESULTS_FILE.exists():
            RESULTS_FILE.unlink()

    return {
        "cleared": True,
        "message": "Experiment results cleared"
    }


# ==========================================
# DETECTION METRICS SUMMARY
# ==========================================

@app.get("/metrics")
def get_metrics():

    rows = read_results()

    delays = []
    changed_counts = []

    for row in rows:

        if row["detection_delay_ms"] not in ("", "None"):
            try:
                delays.append(
                    float(row["detection_delay_ms"])
                )
            except ValueError:
                pass

        if row["files_changed_before_alert"] not in ("", "None"):
            try:
                changed_counts.append(
                    int(row["files_changed_before_alert"])
                )
            except ValueError:
                pass

    average_delay = (
        sum(delays) / len(delays)
        if delays else 0
    )

    average_changed = (
        sum(changed_counts) / len(changed_counts)
        if changed_counts else 0
    )

    return {
        "average_detection_delay_ms":
            round(average_delay, 2),

        "average_files_changed_before_alert":
            round(average_changed, 2),

        "experiments_with_delay":
            len(delays),

        "experiments_with_file_counts":
            len(changed_counts)
    }

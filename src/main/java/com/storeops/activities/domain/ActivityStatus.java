package com.storeops.activities.domain;

/** Lifecycle of an operational activity. */
public enum ActivityStatus {
  PLANNED,
  IN_PROGRESS,
  BLOCKED,
  DONE,
  CANCELLED
}

package io.github.developeranalytics.worker;

import io.github.developeranalytics.domain.job.BackgroundJob;
public interface BackgroundJobHandler { String jobType(); void handle(BackgroundJob job) throws Exception; }

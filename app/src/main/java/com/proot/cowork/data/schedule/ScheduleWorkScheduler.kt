package com.proot.cowork.data.schedule

import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.proot.cowork.domain.schedule.ScheduledTask
import com.proot.cowork.workers.ScheduledAgentWorker
import java.util.concurrent.TimeUnit
import kotlin.math.max

object ScheduleWorkScheduler {

    private const val TAG = "ScheduleWorkScheduler"
    private const val WORK_TAG = "cowork_scheduled_agent"

    fun enqueue(context: Context, task: ScheduledTask) {
        val delayMs = max(0L, task.triggerAtMillis - System.currentTimeMillis())
        val builder = OneTimeWorkRequestBuilder<ScheduledAgentWorker>()
            .setInputData(
                workDataOf(
                    ScheduledAgentWorker.KEY_TASK_ID to task.id,
                    ScheduledAgentWorker.KEY_PROMPT to task.prompt,
                ),
            )
            .addTag(WORK_TAG)

        if (delayMs > 0L) {
            builder.setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
        } else {
            // WorkManager rejects expedited requests that also have an initial delay.
            builder.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        }

        val request = builder.build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(task.id, ExistingWorkPolicy.REPLACE, request)
        Log.i(TAG, "enqueued ${task.id} delayMs=$delayMs")
    }

    fun cancel(context: Context, taskId: String) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(taskId)
    }
}

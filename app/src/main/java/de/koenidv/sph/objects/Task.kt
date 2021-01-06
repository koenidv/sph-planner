package de.koenidv.sph.objects

import java.util.*

//  Created by koenidv on 19.12.2020.
data class Task(
        var taskId: String, // i.e. m_bar_1_task-2020-12-07_48 (48 is sph post id)
        var id_course: String, // Course to be attached to, i.e. m_bar_1
        var id_post: String,// Post to be attached to, i.e. Post id, i.e. m_bar_1_post-2020-12-07_48
        var description: String, // Description of the task. There's no title.
        var date: Date, // Date of the post to be attached to. Should be day only, no time
        var isDone: Boolean, // Whether the task has been completed
        var isPinned: Boolean = false, // If the task is pinned
        var dueDate: Date? = null // Due date, null if none is set
) {
    override fun equals(other: Any?): Boolean {
        if (other is Task)
            return other.taskId == this.taskId
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return taskId.hashCode()
    }
}

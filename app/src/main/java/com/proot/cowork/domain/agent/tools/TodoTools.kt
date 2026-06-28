package com.proot.cowork.domain.agent.tools

import com.proot.cowork.data.todos.TodoStore
import com.proot.cowork.domain.agent.AgentRunContext
import org.json.JSONArray
import org.json.JSONObject

object TodoTools {
    const val NAME_WRITE = "todo_write"
    const val NAME_READ = "todo_read"

    fun writeDefinition(): JSONObject = toolDef(
        NAME_WRITE,
        "Create or replace the todo list for this chat thread. Call before todo_read. Only one item should be in_progress.",
        JSONObject().apply {
            put(
                "todos",
                JSONObject().apply {
                    put("type", "array")
                    put(
                        "items",
                        JSONObject().apply {
                            put("type", "object")
                            put(
                                "properties",
                                JSONObject().apply {
                                    put("id", JSONObject().put("type", "string"))
                                    put("content", JSONObject().put("type", "string"))
                                    put(
                                        "status",
                                        JSONObject()
                                            .put("type", "string")
                                            .put("enum", JSONArray().put("pending").put("in_progress").put("completed")),
                                    )
                                    put("priority", JSONObject().put("type", "string"))
                                },
                            )
                            put("required", JSONArray().put("content"))
                        },
                    )
                },
            )
        },
        listOf("todos"),
    )

    fun readDefinition(): JSONObject = toolDef(
        NAME_READ,
        "Read todos for this thread. Only call after todo_write in the same run.",
        JSONObject(),
        emptyList(),
    )

    suspend fun write(store: TodoStore, args: JSONObject): String {
        val threadId = AgentRunContext.threadId
            ?: return "Error: no active chat thread for todos"
        val todos = TodoStore.parseTodosFromJson(args)
        if (todos.isEmpty()) return "Error: todos array is required"
        return store.writeTodos(threadId, todos)
    }

    suspend fun read(store: TodoStore): String {
        val threadId = AgentRunContext.threadId
            ?: return "Error: no active chat thread for todos"
        return store.readTodos(threadId)
    }

    private fun toolDef(name: String, desc: String, props: JSONObject, required: List<String>): JSONObject =
        JSONObject().apply {
            put("type", "function")
            put("function", JSONObject().apply {
                put("name", name)
                put("description", desc)
                put("parameters", JSONObject().apply {
                    put("type", "object")
                    put("properties", props)
                    if (required.isNotEmpty()) {
                        put("required", JSONArray(required))
                    }
                })
            })
        }
}

package com.proot.cowork.domain.agent

/**
 * Swarm multi-agent UI is shelved until a Kimi/Hermes-style orchestration layer ships.
 * Backend ([CoworkAgentRunner.executeSwarmPlan], [com.proot.cowork.service.AgentExecutionService])
 * remains for future re-enable.
 */
object AgentFeatureFlags {
    const val SWARM_UI_ENABLED = false
}

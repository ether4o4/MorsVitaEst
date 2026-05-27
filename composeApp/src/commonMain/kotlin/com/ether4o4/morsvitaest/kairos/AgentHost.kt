package com.ether4o4.morsvitaest.kairos

import kotlinx.serialization.Serializable

@Serializable
enum class AgentRuntimeKind {
    API,
    LOCAL_MODEL,
    TERMUX,
    POWERSHELL,
    SANDBOX,
    MCP,
    HUMAN,
}

@Serializable
enum class ToolAuthority {
    NONE,
    READ_ONLY,
    APPROVAL_REQUIRED,
    AUTONOMOUS,
}

@Serializable
data class AgentRuntimeEndpoint(
    val kind: AgentRuntimeKind,
    val displayName: String,
    val baseUrl: String? = null,
    val command: String? = null,
    val apiKeyRef: String? = null,
    val workingDirectory: String? = null,
)

@Serializable
data class HostedAgentProfile(
    val id: String,
    val name: String,
    val runtime: AgentRuntimeEndpoint,
    val colorRole: KairosColorRole? = null,
    val toolAuthority: ToolAuthority = ToolAuthority.NONE,
    val allowedToolIds: Set<String> = emptySet(),
    val notes: String = "",
)

@Serializable
data class InfrastructureToolRequest(
    val requestingAgentId: String,
    val toolName: String,
    val argumentsJson: String,
    val reason: String = "",
    val conversationId: String? = null,
)

@Serializable
data class InfrastructureToolDecision(
    val allowed: Boolean,
    val requiresApproval: Boolean = false,
    val reason: String,
)

object AgentHostPolicy {
    fun decideToolExecution(
        agent: HostedAgentProfile,
        request: InfrastructureToolRequest,
    ): InfrastructureToolDecision {
        if (agent.id != request.requestingAgentId) {
            return InfrastructureToolDecision(
                allowed = false,
                reason = "Tool request agent id does not match hosted agent profile.",
            )
        }

        if (agent.toolAuthority == ToolAuthority.NONE) {
            return InfrastructureToolDecision(
                allowed = false,
                reason = "Agent has no tool authority.",
            )
        }

        if (agent.allowedToolIds.isNotEmpty() && request.toolName !in agent.allowedToolIds) {
            return InfrastructureToolDecision(
                allowed = false,
                reason = "Tool is outside this agent's allowlist.",
            )
        }

        return when (agent.toolAuthority) {
            ToolAuthority.NONE -> InfrastructureToolDecision(
                allowed = false,
                reason = "Agent has no tool authority.",
            )

            ToolAuthority.READ_ONLY -> InfrastructureToolDecision(
                allowed = true,
                requiresApproval = false,
                reason = "Read-only tool authority granted by infrastructure policy.",
            )

            ToolAuthority.APPROVAL_REQUIRED -> InfrastructureToolDecision(
                allowed = true,
                requiresApproval = true,
                reason = "Infrastructure policy requires user approval before execution.",
            )

            ToolAuthority.AUTONOMOUS -> InfrastructureToolDecision(
                allowed = true,
                requiresApproval = false,
                reason = "Autonomous tool authority granted by infrastructure policy.",
            )
        }
    }
}

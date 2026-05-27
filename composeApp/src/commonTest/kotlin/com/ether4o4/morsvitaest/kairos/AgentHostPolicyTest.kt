package com.ether4o4.morsvitaest.kairos

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AgentHostPolicyTest {
    @Test
    fun `agent without authority cannot execute tools`() {
        val agent = HostedAgentProfile(
            id = "local-agent",
            name = "Local Agent",
            runtime = AgentRuntimeEndpoint(
                kind = AgentRuntimeKind.LOCAL_MODEL,
                displayName = "Ollama on Termux",
                baseUrl = "http://127.0.0.1:11434",
            ),
            toolAuthority = ToolAuthority.NONE,
        )

        val decision = AgentHostPolicy.decideToolExecution(
            agent,
            InfrastructureToolRequest(
                requestingAgentId = "local-agent",
                toolName = "fetch_url",
                argumentsJson = """{"url":"https://example.com"}""",
            ),
        )

        assertFalse(decision.allowed)
    }

    @Test
    fun `approval authority permits request but marks approval required`() {
        val agent = HostedAgentProfile(
            id = "powershell-agent",
            name = "PowerShell Operator",
            runtime = AgentRuntimeEndpoint(
                kind = AgentRuntimeKind.POWERSHELL,
                displayName = "Windows PowerShell",
                command = "powershell.exe",
            ),
            toolAuthority = ToolAuthority.APPROVAL_REQUIRED,
            allowedToolIds = setOf("execute_shell_command"),
        )

        val decision = AgentHostPolicy.decideToolExecution(
            agent,
            InfrastructureToolRequest(
                requestingAgentId = "powershell-agent",
                toolName = "execute_shell_command",
                argumentsJson = """{"command":"Get-Location"}""",
            ),
        )

        assertTrue(decision.allowed)
        assertTrue(decision.requiresApproval)
    }

    @Test
    fun `allowlist blocks tools outside agent scope`() {
        val agent = HostedAgentProfile(
            id = "termux-agent",
            name = "Termux Agent",
            runtime = AgentRuntimeEndpoint(
                kind = AgentRuntimeKind.TERMUX,
                displayName = "Termux",
                command = "bash",
            ),
            toolAuthority = ToolAuthority.AUTONOMOUS,
            allowedToolIds = setOf("fetch_url"),
        )

        val decision = AgentHostPolicy.decideToolExecution(
            agent,
            InfrastructureToolRequest(
                requestingAgentId = "termux-agent",
                toolName = "execute_shell_command",
                argumentsJson = """{"command":"pwd"}""",
            ),
        )

        assertFalse(decision.allowed)
    }
}

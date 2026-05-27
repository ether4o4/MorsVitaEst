# Kairos Infrastructure

**Last verified:** 2026-05-27

Kairos is the infrastructure layer inside MorsVitaEst. It turns the app from "an assistant that can chat" into a host for agents, runtimes, tools, memory, and background work.

The key rule is simple:

> The LLM may reason about tools, request tools, or explain tool results. The infrastructure owns whether tools exist, whether they are allowed, and how they execute.

That keeps MorsVitaEst useful even when the active agent is a tiny local model, a remote API model, a Termux script, a PowerShell workflow, an MCP server, or a human-approved automation.

## Agent Host

An agent is anything MorsVitaEst can house and route work to.

Supported runtime shapes:

| Runtime | Meaning |
|---|---|
| `API` | Hosted model or agent reached with a provider key or OpenAI-compatible endpoint |
| `LOCAL_MODEL` | Local inference engine such as Ollama, LiteRT, or another device-local model runner |
| `TERMUX` | Android Termux runtime, usually for local shell, scripts, package workflows, and model servers |
| `POWERSHELL` | Windows PowerShell runtime for desktop-side workflows |
| `SANDBOX` | In-app Linux sandbox runtime on Android |
| `MCP` | External tool or agent server exposed through Model Context Protocol |
| `HUMAN` | Human-mediated lane for approval, review, or manual handoff |

Each hosted agent profile has:

- stable id and display name
- runtime endpoint or command metadata
- optional five-color role
- tool authority level
- optional per-agent tool allowlist
- notes for user-facing or internal routing context

The goal is not to force every agent into the same model interface. The goal is to make each runtime visible to one host that can decide what it is allowed to do.

## Infrastructure-Owned Tools

Tools are not owned by the model. They are native capabilities of the MorsVitaEst infrastructure.

The model can produce a request like:

```json
{
  "requestingAgentId": "termux-agent",
  "toolName": "execute_shell_command",
  "argumentsJson": "{\"command\":\"pwd\"}",
  "reason": "Inspect current working directory before running setup."
}
```

The infrastructure then decides:

- Is this agent known?
- Is this tool available on the current platform?
- Is the tool enabled?
- Is the requested tool inside the agent's allowlist?
- Does the request need user approval?
- Should the result be truncated, stored, shown, or routed back into a model turn?

That means small local models do not need perfect tool-call behavior to be useful. They can operate through structured infrastructure affordances, UI actions, explicit workflows, scheduled tasks, or another agent lane that asks for a tool on their behalf.

## Tool Authority

Tool authority is separate from agent intelligence.

| Authority | Behavior |
|---|---|
| `NONE` | Agent can chat or reason but cannot execute tools |
| `READ_ONLY` | Agent can use low-risk read tools without approval |
| `APPROVAL_REQUIRED` | Agent can prepare tool calls, but the infrastructure pauses for user approval |
| `AUTONOMOUS` | Agent can execute allowed tools without an approval stop |

An agent may be powerful but have no authority. Another agent may be simple but allowed to run a narrow tool safely. Authority belongs to the host, not the LLM.

## Five-Color Routing

The color system routes responsibility inside the host:

- **Red**: tool execution, shell, device moves, implementation
- **Blue**: architecture, continuity, risk review
- **Green**: build path, local runtime, integration
- **Yellow**: exploration, candidate capabilities, user-facing clarity
- **Purple**: synthesis, identity, final coherence

The colors are not five unrelated chatbots. They are lanes inside one agent infrastructure.

## Runtime Examples

### Local Termux Agent

- Runtime: `TERMUX`
- Endpoint: local command or local Ollama server
- Tool authority: `APPROVAL_REQUIRED` or narrow `AUTONOMOUS`
- Typical tools: shell, fetch, file inspection, local model health checks

### PowerShell Operator

- Runtime: `POWERSHELL`
- Endpoint: `powershell.exe`
- Tool authority: usually `APPROVAL_REQUIRED`
- Typical tools: repo inspection, build scripts, Windows file operations, deployment helpers

### API Model Agent

- Runtime: `API`
- Endpoint: provider or OpenAI-compatible URL
- Tool authority: depends on trust, cost, and task type
- Typical tools: web fetch, MCP, memory, planning, code review

### MCP Agent

- Runtime: `MCP`
- Endpoint: Streamable HTTP MCP server
- Tool authority: scoped to server permissions and local policy
- Typical tools: docs lookup, search, repo tools, domain-specific actions

## Current Code Boundary

Current primitives:

| File | Purpose |
|---|---|
| `composeApp/src/commonMain/.../data/ToolExecutor.kt` | Executes named tools by JSON arguments, independent from provider-specific model code |
| `composeApp/src/commonMain/.../kairos/AgentHost.kt` | Defines hosted agent profiles, runtime kinds, tool authority, and infrastructure tool requests |
| `composeApp/src/commonMain/.../kairos/KairosSwarm.kt` | Routes requests through the five-color responsibility lanes |
| `composeApp/src/commonMain/.../kairos/CapabilityVetter.kt` | Scores and stages candidate capabilities before they become trusted infrastructure |
| `composeApp/src/commonTest/.../kairos/AgentHostPolicyTest.kt` | Locks in infrastructure-owned tool authority behavior |

## Design Direction

MorsVitaEst should evolve toward this split:

```text
User
  -> MorsVitaEst Host
      -> Agent Router
          -> API Agent
          -> Local Model Agent
          -> Termux Agent
          -> PowerShell Agent
          -> MCP Agent
      -> Tool Authority Layer
          -> Native Tools
          -> Shell/Sandbox
          -> MCP Tools
          -> Memory/Tasks/Heartbeat
      -> Result Router
          -> UI
          -> Conversation
          -> Background Work
```

The host is the durable thing. Agents can be swapped, added, disabled, or routed differently. Tools remain governed by MorsVitaEst.

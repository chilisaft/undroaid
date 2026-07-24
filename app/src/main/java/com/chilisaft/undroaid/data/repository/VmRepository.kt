package com.chilisaft.undroaid.data.repository

import com.apollographql.apollo.ApolloClient
import com.chilisaft.undroaid.data.api.runWidgetMutation
import com.chilisaft.undroaid.data.api.runWidgetQuery
import com.chilisaft.undroaid.data.models.VmDomain
import com.chilisaft.undroaid.data.models.VmRunState
import com.chilisaft.undroaid.data.models.WidgetResult
import com.chilisaft.undroaid.graphql.VmForceStopMutation
import com.chilisaft.undroaid.graphql.VmPauseMutation
import com.chilisaft.undroaid.graphql.VmRebootMutation
import com.chilisaft.undroaid.graphql.VmResumeMutation
import com.chilisaft.undroaid.graphql.VmStartMutation
import com.chilisaft.undroaid.graphql.VmStopMutation
import com.chilisaft.undroaid.graphql.VmsQuery
import com.chilisaft.undroaid.graphql.type.VmState
import javax.inject.Inject

/** Backs the VMs tab's full domain list + actions - same shape as [DockerRepository] for the Docker tab. */
class VmRepository @Inject constructor(
    private val apolloClient: ApolloClient
) {

    suspend fun getVms(): WidgetResult<List<VmDomain>> =
        apolloClient.runWidgetQuery(VmsQuery()) { data -> data.vms.domains.orEmpty().map { it.toVmDomain() } }

    suspend fun startVm(id: String): WidgetResult<Unit> =
        apolloClient.runWidgetMutation(VmStartMutation(id)) { }

    suspend fun stopVm(id: String): WidgetResult<Unit> =
        apolloClient.runWidgetMutation(VmStopMutation(id)) { }

    suspend fun forceStopVm(id: String): WidgetResult<Unit> =
        apolloClient.runWidgetMutation(VmForceStopMutation(id)) { }

    suspend fun pauseVm(id: String): WidgetResult<Unit> =
        apolloClient.runWidgetMutation(VmPauseMutation(id)) { }

    suspend fun resumeVm(id: String): WidgetResult<Unit> =
        apolloClient.runWidgetMutation(VmResumeMutation(id)) { }

    suspend fun rebootVm(id: String): WidgetResult<Unit> =
        apolloClient.runWidgetMutation(VmRebootMutation(id)) { }

    private fun VmsQuery.Domain.toVmDomain() = VmDomain(
        id = id,
        name = name ?: "Unnamed VM",
        state = state.toRunState(),
        statusLabel = state.toLabel()
    )

    private fun VmState.toRunState(): VmRunState = when (this) {
        VmState.RUNNING -> VmRunState.RUNNING
        VmState.PAUSED -> VmRunState.PAUSED
        else -> VmRunState.STOPPED
    }

    private fun VmState.toLabel(): String = when (this) {
        VmState.NOSTATE -> "No State"
        VmState.RUNNING -> "Running"
        VmState.IDLE -> "Idle"
        VmState.PAUSED -> "Paused"
        VmState.SHUTDOWN -> "Shutting Down"
        VmState.SHUTOFF -> "Shut Off"
        VmState.CRASHED -> "Crashed"
        VmState.PMSUSPENDED -> "Suspended"
        VmState.UNKNOWN__ -> "Unknown"
    }
}

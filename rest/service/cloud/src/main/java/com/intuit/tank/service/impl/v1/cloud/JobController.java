/**
 * Copyright 2011 Intuit Inc. All Rights Reserved
 */
package com.intuit.tank.service.impl.v1.cloud;

/*
 * #%L
 * Cloud Rest Service
 * %%
 * Copyright (C) 2011 - 2015 Intuit Inc.
 * %%
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * #L%
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Named;

import com.amazonaws.xray.AWSXRay;
import com.intuit.tank.api.cloud.VMTracker;
import com.intuit.tank.api.model.v1.cloud.CloudVmStatus;
import com.intuit.tank.api.model.v1.cloud.CloudVmStatusContainer;
import com.intuit.tank.api.model.v1.cloud.VMStatus;
import com.intuit.tank.dao.JobInstanceDao;
import com.intuit.tank.dao.WorkloadDao;
import com.intuit.tank.dao.util.ProjectDaoUtil;
import com.intuit.tank.harness.data.HDWorkload;
import com.intuit.tank.perfManager.workLoads.JobManager;
import com.intuit.tank.project.JobInstance;
import com.intuit.tank.project.Workload;
import com.intuit.tank.transform.scriptGenerator.ConverterUtil;
import com.intuit.tank.vm.api.enumerated.JobLifecycleEvent;
import com.intuit.tank.vm.api.enumerated.JobQueueStatus;
import com.intuit.tank.vm.api.enumerated.JobStatus;
import com.intuit.tank.vm.api.enumerated.VMRegion;
import com.intuit.tank.vm.event.JobEvent;
import com.intuit.tank.vm.perfManager.AgentChannel;
import com.intuit.tank.vm.settings.TankConfig;
import com.intuit.tank.vmManager.environment.amazon.AmazonInstance;

/**
 * CloudServiceV1
 * 
 * @author dangleton
 * 
 */
@Named
@RequestScoped
public class JobController {

    @Inject
    private VMTracker vmTracker;

    @Inject
    private JobManager jobManager;

    @Inject
    private AgentChannel agentChannel;

    @Inject
    private Event<JobEvent> jobEventProducer;

    @Inject
    private CloudController cloudController;

    /**
     * {@inheritDoc}
     */
    public String startJob(String jobId) {
        AWSXRay.beginSubsegment("Start.Job." + jobId);
        JobInstanceDao jobInstanceDao = new JobInstanceDao();
        JobInstance job = jobInstanceDao.findById(Integer.valueOf(jobId));
        synchronized (jobId) {
            if (job.getStatus() == JobQueueStatus.Created) {// only start if new job
                // save the job
                job.setStatus(JobQueueStatus.Starting);
                jobInstanceDao.saveOrUpdate(job);

                ProjectDaoUtil.storeScriptFile(jobId, getScriptString(job));

                vmTracker.removeStatusForJob(jobId);
                jobManager.startJob(job.getId());
                jobEventProducer.fire(new JobEvent(jobId, "", JobLifecycleEvent.JOB_STARTED));
            }
        }
        AWSXRay.endSubsegment();
        return jobId;
    }

    /**
     * Use the AWS SDK to terminate instances.
     * If no instances can be found, set jobStatus to Completed
     */
    public void killJob(String jobId, boolean fireEvent) {
        List<String> instanceIds = getInstancesForJob(jobId);
        vmTracker.stopJob(jobId);
        if (instanceIds.isEmpty()) {
            JobInstanceDao dao = new JobInstanceDao();
            JobInstance job = dao.findById(Integer.parseInt(jobId));
            if (job != null) {
                job.setStatus(JobQueueStatus.Completed);
                dao.saveOrUpdate(job);
            }
        } else {
            killInstances(instanceIds);
        }

        if (fireEvent) {
            jobEventProducer.fire(new JobEvent(jobId, "", JobLifecycleEvent.JOB_KILLED));
        }
    }

    /**
     * {@inheritDoc}
     */
    public void killJob(String jobId) {
        killJob(jobId, true);
    }
    
    /**
     * {@inheritDoc}
     */
    public Set<CloudVmStatusContainer> killAllJobs() {
    	Set<CloudVmStatusContainer> jobs = vmTracker.getAllJobs();
        for (CloudVmStatusContainer job : jobs) {
            String jobId = job.getJobId();
            killJob(jobId, true);
        }
    	return jobs;
    }

    /**
     * {@inheritDoc}
     */
    public void killInstance(String instanceId) { killInstances(Collections.singletonList(instanceId)); }

    /**
     * {@inheritDoc}
     */
    public void killInstances(List<String> instanceIds) {
        agentChannel.killAgents(instanceIds);

        if (!vmTracker.isDevMode()) {
            for (VMRegion region : new TankConfig().getVmManagerConfig().getRegions()) {
                AmazonInstance amzInstance = new AmazonInstance(region);
                amzInstance.killInstances(instanceIds);
            }
        }
        String jobId = null;
        for (String instanceId : instanceIds) {
            CloudVmStatus status = new CloudVmStatus(vmTracker.getStatus(instanceId));
            status.setCurrentUsers(0);
            status.setEndTime(new Date());
            status.setJobStatus(JobStatus.Completed);
            status.setVmStatus(VMStatus.terminated);
            vmTracker.setStatus(status);
            jobId = status.getJobId();
        }
        if (jobId != null) {
            cloudController.checkJobStatus(jobId);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Set<CloudVmStatusContainer> stopAllJobs() {
    	Set<CloudVmStatusContainer> jobs = vmTracker.getAllJobs();
        for (CloudVmStatusContainer job : jobs) {
            String jobId = (job).getJobId();
            List<String> instanceIds = getInstancesForJob(jobId);
            vmTracker.stopJob(jobId);
            stopAgents(instanceIds);
            jobEventProducer.fire(new JobEvent(jobId, "", JobLifecycleEvent.JOB_STOPPED));
        }
    	return jobs;
    }
    
    /**
     * {@inheritDoc}
     */
    public void stopJob(String jobId) {
        List<String> instanceIds = getInstancesForJob(jobId);
        vmTracker.stopJob(jobId);
        stopAgents(instanceIds);
        jobEventProducer.fire(new JobEvent(jobId, "", JobLifecycleEvent.JOB_STOPPED));
    }

    /**
     * {@inheritDoc}
     */
    public void stopAgent(String instanceId) {
        stopAgents(Collections.singletonList(instanceId));

    }

    /**
     * {@inheritDoc}
     */
    public void stopAgents(List<String> instanceIds) {
        agentChannel.stopAgents(instanceIds);
    }

    /**
     * {@inheritDoc}
     */
    public void pauseJob(String jobId) {
        List<String> instanceIds = getInstancesForJob(jobId);
        pauseAgents(instanceIds);
        jobEventProducer.fire(new JobEvent(jobId, "", JobLifecycleEvent.JOB_PAUSED));
    }

    /**
     * {@inheritDoc}
     */
    public void pauseAgent(String instanceId) {
        pauseAgents(Collections.singletonList(instanceId));
    }

    /**
     * {@inheritDoc}
     */
    public void pauseAgents(List<String> instanceIds) {
        agentChannel.pauseAgents(instanceIds);
    }

    /**
     * {@inheritDoc}
     */
    public void restartJob(String jobId) {
        List<String> instanceIds = getInstancesForJob(jobId);
        restartAgents(instanceIds);
        jobEventProducer.fire(new JobEvent(jobId, "", JobLifecycleEvent.JOB_RESUMED));
    }

    /**
     * {@inheritDoc}
     */
    public void restartAgent(String instanceId) {
        restartAgents(Collections.singletonList(instanceId));
    }

    /**
     * {@inheritDoc}
     */
    public void restartAgents(List<String> instanceIds) {
        agentChannel.restartAgents(instanceIds);
    }

    /**
     * {@inheritDoc}
     */
    public void pauseRampInstance(String instanceId) {
        pauseRampInstances(Collections.singletonList(instanceId));
    }

    /**
     * {@inheritDoc}
     */
    public void pauseRampJob(String jobId) {
        List<String> instanceIds = getInstancesForJob(jobId);
        pauseRampInstances(instanceIds);
        jobEventProducer.fire(new JobEvent(jobId, "", JobLifecycleEvent.RAMP_PAUSED));
    }

    /**
     * {@inheritDoc}
     */
    public void pauseRampInstances(List<String> instanceIds) {
        agentChannel.pauseRamp(instanceIds);
    }

    /**
     * {@inheritDoc}
     */
    public void resumeRampInstance(String instanceId) {
        resumeRampInstances(Collections.singletonList(instanceId));
    }

    /**
     * {@inheritDoc}
     */
    public void resumeRampJob(String jobId) {
        List<String> instanceIds = getInstancesForJob(jobId);
        resumeRampInstances(instanceIds);
        jobEventProducer.fire(new JobEvent(jobId, "", JobLifecycleEvent.RAMP_RESUMED));
    }

    /**
     * {@inheritDoc}
     */
    public void resumeRampInstances(List<String> instanceIds) {
        agentChannel.resumeRamp(instanceIds);
    }

    /**
     * @param jobId
     * @return
     */
    private List<String> getInstancesForJob(String jobId) {
        List<String> instanceIds = new ArrayList<String>();
        CloudVmStatusContainer statuses = vmTracker.getVmStatusForJob(jobId);
        if (statuses != null) {
            instanceIds = statuses.getStatuses().stream().map(CloudVmStatus::getInstanceId).collect(Collectors.toList());
        }
        return instanceIds;
    }

    /**
     * @param job
     * @return
     */
    public static String getScriptString(JobInstance job) {
        WorkloadDao dao = new WorkloadDao();
        Workload workload = dao.findById(job.getWorkloadId());
        workload.getTestPlans();
        dao.loadScriptsForWorkload(workload);
        HDWorkload hdWorkload = ConverterUtil.convertWorkload(workload, job);
        return ConverterUtil.getWorkloadXML(hdWorkload);
    }

}

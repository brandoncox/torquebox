/*
 * Copyright 2008-2011 Red Hat, Inc, and individual contributors.
 * 
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.torquebox.core.pool;

import java.util.List;

import org.jboss.as.server.deployment.DeploymentException;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jruby.Ruby;
import org.torquebox.core.app.RubyApplicationMetaData;
import org.torquebox.core.as.CoreServices;
import org.torquebox.core.as.services.DefaultRubyRuntimePoolService;
import org.torquebox.core.as.services.SharedRubyRuntimeFactoryPoolService;
import org.torquebox.core.runtime.BasicRubyRuntimePoolMBean;
import org.torquebox.core.runtime.DefaultRubyRuntimePool;
import org.torquebox.core.runtime.DefaultRubyRuntimePoolMBean;
import org.torquebox.core.runtime.PoolMetaData;
import org.torquebox.core.runtime.RubyRuntimeFactory;
import org.torquebox.core.runtime.RubyRuntimePool;
import org.torquebox.core.runtime.SharedRubyRuntimePool;

/**
 * <pre>
 * Stage: REAL
 *    In: PoolMetaData, DeployerRuby
 *   Out: RubyRuntimePool
 * </pre>
 * 
 * Creates the proper RubyRuntimePool as specified by the PoolMetaData
 */
public class RuntimePoolDeployer implements DeploymentUnitProcessor {

    public RuntimePoolDeployer() {
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit unit = phaseContext.getDeploymentUnit();
        List<PoolMetaData> allAttachments = unit.getAttachmentList( PoolMetaData.ATTACHMENTS_KEY );

        for (PoolMetaData each : allAttachments) {
            deploy( phaseContext, each );
        }
    }

    protected void deploy(DeploymentPhaseContext phaseContext, PoolMetaData poolMetaData) {
        log.info( "Deploying runtime pool: " + poolMetaData );
        DeploymentUnit unit = phaseContext.getDeploymentUnit();
        RubyApplicationMetaData rubyAppMetaData = unit.getAttachment( RubyApplicationMetaData.ATTACHMENT_KEY );
        String deploymentName = unit.getName();

        /*
         * if (poolMetaData.isGlobal()) { SharedRubyRuntimePool pool = new
         * SharedRubyRuntimePool(); pool.setName( poolMetaData.getName() );
         * 
         * String instanceName = poolMetaData.getInstanceName(); if
         * (instanceName == null) { try { Ruby runtime = unit.getAttachment(
         * DeployerRuby.class ).getRuby(); builder.addConstructorParameter(
         * Ruby.class.getName(), runtime ); } catch (Exception e) { throw new
         * DeploymentException( e ); } } else { ValueMetaData runtimeInjection =
         * builder.createInject( instanceName );
         * builder.addConstructorParameter( Ruby.class.getName(),
         * runtimeInjection ); } } else
         */
        if (poolMetaData.isShared()) {
            SharedRubyRuntimePool pool = new SharedRubyRuntimePool();
            pool.setName( poolMetaData.getName() );

            SharedRubyRuntimeFactoryPoolService service = new SharedRubyRuntimeFactoryPoolService( pool );

            ServiceName name = CoreServices.runtimePoolName( deploymentName, pool.getName() );
            ServiceBuilder<RubyRuntimePool> builder = phaseContext.getServiceTarget().addService( name, service );
            builder.addDependency( CoreServices.runtimeFactoryName( deploymentName ), RubyRuntimeFactory.class, service.getRubyRuntimeFactoryInjector() );
            builder.install();
        } else {
            DefaultRubyRuntimePool pool = new DefaultRubyRuntimePool();

            pool.setName( poolMetaData.getName() );
            pool.setMinimumInstances( poolMetaData.getMaximumSize() );
            pool.setMaximumInstances( poolMetaData.getMaximumSize() );

            DefaultRubyRuntimePoolService service = new DefaultRubyRuntimePoolService( pool );

            ServiceName name = CoreServices.runtimePoolName( deploymentName, pool.getName() );
            ServiceBuilder<RubyRuntimePool> builder = phaseContext.getServiceTarget().addService( name, service );
            builder.addDependency( CoreServices.runtimeFactoryName( deploymentName ), RubyRuntimeFactory.class, service.getRubyRuntimeFactoryInjector() );
            builder.install();
        }
    }

    @Override
    public void undeploy(org.jboss.as.server.deployment.DeploymentUnit context) {
        // TODO Auto-generated method stub

    }

    private static final Logger log = Logger.getLogger( "org.torquebox.core.pool" );
}

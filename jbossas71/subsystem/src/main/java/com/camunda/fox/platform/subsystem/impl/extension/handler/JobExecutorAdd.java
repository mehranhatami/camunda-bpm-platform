/**
 * Copyright (C) 2011, 2012 camunda services GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.camunda.fox.platform.subsystem.impl.extension.handler;

import static com.camunda.fox.platform.subsystem.impl.extension.ModelConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;

import java.util.List;
import java.util.Locale;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;

import com.camunda.fox.platform.subsystem.impl.platform.ContainerJobExecutorService;

/**
 * Provides the description and the implementation of the process-engine#add operation.
 * 
 */
public class JobExecutorAdd extends AbstractBoottimeAddStepHandler implements DescriptionProvider {
    
  public static final JobExecutorAdd INSTANCE = new JobExecutorAdd();

  public ModelNode getModelDescription(Locale locale) {
    ModelNode node = new ModelNode();
    node.get(DESCRIPTION).set("Adds a job executor");
    node.get(OPERATION_NAME).set(ADD);
    
    node.get(REQUEST_PROPERTIES, NAME, DESCRIPTION).set("Thread pool name for global job executor");
    node.get(REQUEST_PROPERTIES, NAME, TYPE).set(ModelType.STRING);
    node.get(REQUEST_PROPERTIES, NAME, REQUIRED).set(true);
    
    return node;
  }

  @Override
  protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
      String jobExecutorThreadPoolName = "default";
      if (operation.hasDefined(NAME)) {
         jobExecutorThreadPoolName = operation.get(NAME).asString();
      }
      
      model.get(NAME).set(jobExecutorThreadPoolName);
  }
  
  
  @Override
  protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model,
          ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers)
          throws OperationFailedException {
    
    String jobExecutorThreadPoolName = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS)).getLastElement().getValue();

    newControllers.add(ContainerJobExecutorService.addService(context.getServiceTarget(), verificationHandler, jobExecutorThreadPoolName));
    
  }

}
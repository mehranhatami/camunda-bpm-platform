/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.bpm.application.ejb;

import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.camunda.fox.client.impl.ProcessApplication;
import com.camunda.fox.platform.FoxPlatformException;
import com.camunda.fox.platform.api.ProcessArchiveService;
import com.camunda.fox.platform.api.ProcessEngineService;

/**
 * @author Daniel Meyer
 * 
 */
public abstract class EjbProcessApplication extends ProcessApplication {

  public static final String PROCESS_ARCHIVE_SERVICE_NAME = "java:global/" +
            "camunda-fox-platform/" +
            "process-engine/" +
            "PlatformService!com.camunda.fox.platform.api.ProcessArchiveService";
  
  public static final String PROCESS_ENGINE_SERVICE_NAME = "java:global/" +
            "camunda-fox-platform/" +
            "process-engine/" +
            "PlatformService!com.camunda.fox.platform.api.ProcessEngineService";

  private static final Logger log = Logger.getLogger(EjbProcessApplication.class.getName());

  protected String javaEeApplicationName;
  
  @Override
  public void start() {
    javaEeApplicationName = autodetectApplicationName();
    super.start();
  }
  
  @Override
  public String getProcessApplicationName() {
    return javaEeApplicationName;

  }

  protected String autodetectApplicationName() {
    try {

      InitialContext initialContext = new InitialContext();

      String appName = (String) initialContext.lookup("java:app/AppName");
      String moduleName = (String) initialContext.lookup("java:module/ModuleName");

      String detectedName = null;
      if (moduleName != null && !moduleName.equals(appName)) {
        // make sure that if an EAR carries multiple PAs, they are correctly
        // identified by appName + moduleName
        detectedName = appName + "/" + moduleName;
      } else {
        detectedName = appName;
      }

      log.info("No name specified for process application in '" + PROCESSES_XML_FILE_LOCATION + "'. Using Java EE application name '" + detectedName + "'.");

      return detectedName;

    } catch (NamingException e) {
      throw new FoxPlatformException("Could not autodetect EjbProcessApplicationName: "+e.getMessage(), e);
    }
  }
  
  /**
   * Lookup Session Object
   */
  protected ProcessApplication getReference() {
    // compose name:
    Class<? extends EjbProcessApplication> applicationClass = getClass();
    String name = "java:global/" + javaEeApplicationName + "/" + applicationClass.getSimpleName() + "!" + applicationClass.getName();    
    try {
      return InitialContext.doLookup(name);
    } catch (NamingException e) {     
      throw new FoxPlatformException("Could not autodetect refernce to EjbProcess Application using name "+name, e);
    }
  }
  
  protected void lookupProcessEngineService() {
    try {
      this.processEngineService = (ProcessEngineService) InitialContext.doLookup(PROCESS_ENGINE_SERVICE_NAME);
    } catch (NamingException e) {
      throw new FoxPlatformException("Could not lookup EJB ProcessEngineService "+e.getMessage(), e);
    }    
  }
  
  protected void lookupArchiveService() {
    try {
      this.processArchiveService = (ProcessArchiveService) InitialContext.doLookup(PROCESS_ARCHIVE_SERVICE_NAME);
    } catch (NamingException e) {
      throw new FoxPlatformException("Could not lookup EJB ProcessEngineService "+e.getMessage(), e);
    }    
  }

}
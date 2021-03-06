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
package org.camunda.bpm.engine.test.concurrency;

import java.util.Collections;
import org.camunda.bpm.engine.OptimisticLockingException;
import org.camunda.bpm.engine.impl.cmd.SetTaskVariablesCmd;
import org.camunda.bpm.engine.impl.test.PluggableProcessEngineTestCase;
import org.camunda.bpm.engine.test.Deployment;

/**
 * @author Daniel Meyer
 *
 */
public class ConcurrentVariableUpdateTest extends PluggableProcessEngineTestCase {

  static ControllableThread activeThread;

  class SetTaskVariablesThread extends ControllableThread {

    OptimisticLockingException exception;

    protected Object variableValue;
    protected String taskId;
    protected String variableName;

    public SetTaskVariablesThread(String taskId, String variableName, Object variableValue) {
      this.taskId = taskId;
      this.variableName = variableName;
      this.variableValue = variableValue;
    }

    public synchronized void startAndWaitUntilControlIsReturned() {
      activeThread = this;
      super.startAndWaitUntilControlIsReturned();
    }

    public void run() {
      try {
        processEngineConfiguration
          .getCommandExecutorTxRequired()
          .execute(new ControlledCommand(activeThread, new SetTaskVariablesCmd(taskId, Collections.singletonMap(variableName, variableValue), false)));

      } catch (OptimisticLockingException e) {
        this.exception = e;
      }
      log.fine(getName()+" ends");
    }
  }

  @Deployment(resources="org/camunda/bpm/engine/test/concurrency/ConcurrentVariableUpdateTest.process.bpmn20.xml")
  public void FAILING_testConcurrentVariableCreate() {

    runtimeService.startProcessInstanceByKey("testProcess");

    String taskId = taskService.createTaskQuery().singleResult().getId();
    String variableName = "varName";

    SetTaskVariablesThread thread1 = new SetTaskVariablesThread(taskId, variableName, "someString");
    thread1.startAndWaitUntilControlIsReturned();

    // this should fail with integrity constraint violation - BUT it doesn't!
    // we end up with two lines in the ACT_RU_VARIABLE table for the same variableName and same executionId...
    SetTaskVariablesThread thread2 = new SetTaskVariablesThread(taskId, variableName, "someString");
    thread2.startAndWaitUntilControlIsReturned();

    thread1.proceedAndWaitTillDone();
    thread2.proceedAndWaitTillDone();

    // fails with FK violation because one of the variables is not deleted.
    taskService.complete(taskId);
  }

  @Deployment(resources="org/camunda/bpm/engine/test/concurrency/ConcurrentVariableUpdateTest.process.bpmn20.xml")
  public void testConcurrentVariableUpdate() {

    runtimeService.startProcessInstanceByKey("testProcess");

    String taskId = taskService.createTaskQuery().singleResult().getId();
    String variableName = "varName";

    taskService.setVariable(taskId, variableName, "someValue");

    SetTaskVariablesThread thread1 = new SetTaskVariablesThread(taskId, variableName, "someString");
    thread1.startAndWaitUntilControlIsReturned();

    // this fails with an optimistic locking exception
    SetTaskVariablesThread thread2 = new SetTaskVariablesThread(taskId, variableName, "someOtherString");
    thread2.startAndWaitUntilControlIsReturned();

    thread1.proceedAndWaitTillDone();
    thread2.proceedAndWaitTillDone();

    assertNull(thread1.exception);
    assertNotNull(thread2.exception);

    // succeeds
    taskService.complete(taskId);
  }


  @Deployment(resources="org/camunda/bpm/engine/test/concurrency/ConcurrentVariableUpdateTest.process.bpmn20.xml")
  public void testConcurrentVariableUpdateTypeChange() {

    runtimeService.startProcessInstanceByKey("testProcess");

    String taskId = taskService.createTaskQuery().singleResult().getId();
    String variableName = "varName";

    taskService.setVariable(taskId, variableName, "someValue");

    SetTaskVariablesThread thread1 = new SetTaskVariablesThread(taskId, variableName, 100l);
    thread1.startAndWaitUntilControlIsReturned();

    // this fails with an optimistic locking exception
    SetTaskVariablesThread thread2 = new SetTaskVariablesThread(taskId, variableName, "someOtherString");
    thread2.startAndWaitUntilControlIsReturned();

    thread1.proceedAndWaitTillDone();
    thread2.proceedAndWaitTillDone();

    assertNull(thread1.exception);
    assertNotNull(thread2.exception);

    // succeeds
    taskService.complete(taskId);
  }

}

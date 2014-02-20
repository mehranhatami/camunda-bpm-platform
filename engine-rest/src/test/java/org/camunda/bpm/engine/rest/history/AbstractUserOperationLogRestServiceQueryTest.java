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
package org.camunda.bpm.engine.rest.history;

import com.jayway.restassured.response.Response;
import org.camunda.bpm.engine.history.UserOperationLogEntry;
import org.camunda.bpm.engine.history.UserOperationLogQuery;
import org.camunda.bpm.engine.rest.AbstractRestServiceTest;
import org.camunda.bpm.engine.rest.dto.history.UserOperationLogEntryDto;
import org.camunda.bpm.engine.rest.helper.MockProvider;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response.Status;
import java.util.List;

import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.path.json.JsonPath.from;
import static org.camunda.bpm.engine.history.UserOperationLogEntry.ENTITY_TYPE_TASK;
import static org.camunda.bpm.engine.history.UserOperationLogEntry.OPERATION_TYPE_CLAIM;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @author Danny Gräf
 */
public abstract class AbstractUserOperationLogRestServiceQueryTest extends AbstractRestServiceTest {

  protected static final String USER_OPERATION_LOG_RESOURCE_URL = TEST_RESOURCE_ROOT_PATH + "/history/user-operation";

  protected static final String USER_OPERATION_LOG_COUNT_RESOURCE_URL = USER_OPERATION_LOG_RESOURCE_URL + "/count";

  protected UserOperationLogQuery queryMock;

  @Before
  public void setUpRuntimeData() {
    List<UserOperationLogEntry> entries = MockProvider.createUserOperationLogEntries();
    queryMock = mock(UserOperationLogQuery.class);
    when(queryMock.list()).thenReturn(entries);
    when(queryMock.count()).thenReturn((long) entries.size());
    when(processEngine.getHistoryService().createUserOperationLogQuery()).thenReturn(queryMock);
  }

  @Test
  public void testQueryCount() {
    expect().statusCode(Status.OK.getStatusCode())
        .body("count", equalTo(1))
        .when().get(USER_OPERATION_LOG_COUNT_RESOURCE_URL);

    verify(queryMock).count();
  }

  @Test
  public void testEmptyQuery() {
    expect().statusCode(Status.OK.getStatusCode())
        .when().get(USER_OPERATION_LOG_RESOURCE_URL);

    verify(queryMock, never()).processDefinitionId(null);
    verify(queryMock, never()).processInstanceId(null);
    verify(queryMock, never()).executionId(null);
    verify(queryMock, never()).taskId(null);
    verify(queryMock, never()).userId(null);
    verify(queryMock, never()).operationId(null);
    verify(queryMock, never()).operationType(null);
    verify(queryMock, never()).entityType(null);
    verify(queryMock, never()).property(null);
    verify(queryMock).list();
  }

  @Test
  public void testQueryParameter() {
    Response response = given()
        .queryParam("processDefinitionId", "1")
        .queryParam("processInstanceId", "2")
        .queryParam("executionId", "3")
        .queryParam("taskId", "4")
        .queryParam("userId", "icke")
        .queryParam("operationId", "5")
        .queryParam("operationType", OPERATION_TYPE_CLAIM)
        .queryParam("entityType", ENTITY_TYPE_TASK)
        .queryParam("property", "owner")
        .then().expect().statusCode(Status.OK.getStatusCode())
        .when().get(USER_OPERATION_LOG_RESOURCE_URL);

    verify(queryMock).processDefinitionId("1");
    verify(queryMock).processInstanceId("2");
    verify(queryMock).executionId("3");
    verify(queryMock).taskId("4");
    verify(queryMock).userId("icke");
    verify(queryMock).operationId("5");
    verify(queryMock).operationType(OPERATION_TYPE_CLAIM);
    verify(queryMock).entityType(ENTITY_TYPE_TASK);
    verify(queryMock).property("owner");
    verify(queryMock).list();

    UserOperationLogEntryDto actual = from(response.asString()).getObject("[0]", UserOperationLogEntryDto.class);
    assertEquals(MockProvider.EXAMPLE_USER_OPERATION_LOG_ID, actual.getId());
    assertEquals(MockProvider.EXAMPLE_PROCESS_DEFINITION_ID, actual.getProcessDefinitionId());
    assertEquals(MockProvider.EXAMPLE_PROCESS_INSTANCE_ID, actual.getProcessInstanceId());
    assertEquals(MockProvider.EXAMPLE_EXECUTION_ID, actual.getExecutionId());
    assertEquals(MockProvider.EXAMPLE_TASK_ID, actual.getTaskId());
    assertEquals(MockProvider.EXAMPLE_USER_ID, actual.getUserId());
    assertEquals(MockProvider.EXAMPLE_USER_OPERATION_TIMESTAMP, actual.getTimestamp());
    assertEquals(MockProvider.EXAMPLE_USER_OPERATION_ID, actual.getOperationId());
    assertEquals(MockProvider.EXAMPLE_USER_OPERATION_TYPE, actual.getOperationType());
    assertEquals(MockProvider.EXAMPLE_USER_OPERATION_ENTITY, actual.getEntityType());
    assertEquals(MockProvider.EXAMPLE_USER_OPERATION_PROPERTY, actual.getProperty());
    assertEquals(MockProvider.EXAMPLE_USER_OPERATION_ORG_VALUE, actual.getOrgValue());
    assertEquals(MockProvider.EXAMPLE_USER_OPERATION_NEW_VALUE, actual.getNewValue());
  }
}

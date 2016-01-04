import com.atlassian.greenhopper.service.sprint.Sprint
import com.atlassian.jira.bc.project.component.ProjectComponent
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.issue.IssueEvent
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.ModifiedValue
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder
import com.atlassian.jira.plugin.webfragment.model.JiraHelper
import com.opensymphony.workflow.WorkflowContext
import com.atlassian.greenhopper.*
import org.joda.time.DateTime

import javax.servlet.http.HttpServletRequest


//method retrieves the current user
def getCurrentApplicationUser() {
    //determine current user

    //Security
    jac = ComponentAccessor.getJiraAuthenticationContext()

    def CurrentUser

    CurrentUser = jac.getUser()


    return CurrentUser
}

//this method creates a comment
def addComment(String myComment) {

    cmm = ComponentAccessor.getCommentManager()

    //cmm.create(issue,getCurrentApplicationUser(),myComment,true)
    cmm.create(getCurrentIssueAfterEvent(),getCurrentApplicationUser(),myComment,true)
    issue.store()
}


//this method gets the value of a customfield value by its name
def getCustomFieldValue(String myCustomField) {

    cfm = ComponentAccessor.getCustomFieldManager()

    CustomField customField = cfm.getCustomFieldObjectByName(myCustomField);
    return  (String)customField.getValue(issue);

}

// this method returns a customfield
def getCustomFieldByName(String myCustomFieldName) {

    cfm = ComponentAccessor.getCustomFieldManager()

    CustomField myCustomField = cfm.getCustomFieldObjectByName(myCustomFieldName);
    return  myCustomField

}


//this method gets a list of subtasks of an issue, retrieves their summary and checks if a defined one exists in this list.
def checkIfSubTaskSummaryExists(String mySummaryToBeChecked) {

    //we create a list of all subtasks for the active issue
    def subTasks = issue.getSubTaskObjects()


    //we create a list of all summaries of all found subtasks
    def subTasksSummaries = []

    subTasks.each {

        subTasksSummaries.add(it.getSummary())
    }

    //we check if in the list of summaries  o
    def checkResult  = subTasksSummaries.contains(mySummaryToBeChecked)

    return checkResult
}


//this method is responsible for the creation of subTask
def addSubTask(String subTaskName, String subTaskDescription) {

    //Instanzierung der factories
    isf = ComponentAccessor.getIssueFactory()

    //IssueFactory: we create her a generic issue
    def issueObject = isf.getIssue()

    issueObject.setProject(issue.getProject())

    //Possible IssueTypeValues are 10001 story, 10101 subtask, 10102 bug, 10000 epic
    // old value 5 ?
    issueObject.setIssueTypeId('10101')

    //getValues of current issue = parent
    issueObject.setParentId(issue.getId())
    issueObject.setSummary(subTaskName + ': user story ' + issue.getSummary())
    issueObject.setAssignee(issue.getAssignee())
    issueObject.setDescription(subTaskDescription)
    issueObject.setReporter(issue.getReporter())



    //here we check if the value for the summary of a subtasks has already been used. We do not want to have
    //two subtasks with the same value.
    def toBeCreatedSubTaskSummary = subTaskName + ': user story ' + issue.getSummary()
    checkResult = checkIfSubTaskSummaryExists(toBeCreatedSubTaskSummary)

    // we only create our new SubTask if the the value of summary does not exist in any already defined subtask
    if (!checkResult) {

        //the issue gets created with the IssueMngr
        ism = ComponentAccessor.getIssueManager()

        //Security
        jac = ComponentAccessor.getJiraAuthenticationContext()

        subTask = ism.createIssueObject(jac.getUser(), issueObject)


        //the created subtask is linked to the issue.This is done through the SubTaskMngr

        stm = ComponentAccessor.getSubTaskManager()
        stm.createSubTaskIssueLink(issue, subTask, jac.getUser())


        // disable the watcher using the WatcherManager
        wtm = ComponentAccessor.getWatcherManager()
        wtm.stopWatching(jac.getUser(), subTask)

    }

}


def check(String myvalue){
    def result = ""

        if (checkIfSubTaskSummaryExists(myvalue)) {
            result = 'vorhanden'
        }

        else {

         result = 'nicht vorhanden'
        }
    }



//Method retrieves the Fixed Version value of the current issue
def getRelease(){

    MutableIssue myMutableIssue = (MutableIssue)issue;
    ArrayList myListReleases = (ArrayList)myMutableIssue.getFixVersions()

    def release = ""

    if(myListReleases!=null){



        //we only consider getting the first item, even though more fix versions can be assigned to an issue
        release = (String)myListReleases[0]

    }

    return release
}

// method reetrieves the assigend sprint of an issue
def getSprintName(){

    ArrayList<Sprint> list = (ArrayList<Sprint>) ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Sprint").getValue(issue);

    def SprintName =""

   if(list!=null){


        list.each {

            it.getName()

            SprintName = it.getName()

           // addComment(sprintName)

        }
    }


   else{
        //do something else

    }

    return SprintName
}

def setCustomFieldValue(Issue issue, String myValueToSave, CustomField myCustomField){

    def MutableIssue myMutableIssue = (MutableIssue)issue

    myMutableIssue.setCustomFieldValue(myCustomField,myValueToSave)


    Map<String,ModifiedValue> modifiedfields = myMutableIssue.getModifiedFields()

    FieldLayoutItem myFieldLayoutItem = ComponentAccessor.getFieldLayoutManager().getFieldLayout(myMutableIssue).getFieldLayoutItem(myCustomField)

    DefaultIssueChangeHolder myDefaultIssueChangeHolder = new DefaultIssueChangeHolder()

    final ModifiedValue myModifiedValue = modifiedfields.get(myCustomField.getId())

    myCustomField.updateValue(myFieldLayoutItem,myMutableIssue,myModifiedValue,myDefaultIssueChangeHolder)


}

// This method retrieves the issue based on its key
def getIssueByKey(String myIssueKey){


    MutableIssue myMutableIssue = ComponentAccessor.getIssueManager().getIssueByCurrentKey(myIssueKey);

    return myMutableIssue
}

def getComponentName(Issue myIssue){

    def MutableIssue myMutableIssue = (MutableIssue)myIssue

    ArrayList<ProjectComponent> myComponents = (ArrayList<ProjectComponent>)myMutableIssue.getComponentObjects()

    def myComponentName = ""

    if (myComponents!=null){

        //we only retrieve the first assigned component.
        myComponentName = (String)myComponents[0].getName()

    }


    return myComponentName

}

def getTodaysDate(){
    def today = new Date()
    return today.toString()
}

//retrieves the current issue i.e. for a listener
def getCurrentIssueAfterEvent(){

    def event = event as IssueEvent
    event.getIssue()
}

//****************

//addComment(getCurrentUser())
//addComment(getCustomFieldValue("BusinessRequestor"))
//addComment('great comment')
//addSubTask('b','my b description')
//addComment(getRelease())
//addComment(getSprintName())
//setCustomFieldValue(issue,getRelease(),getCustomFieldByName('BusinessRequestor'))
//addComment(getComponentName(issue))
//getComponentName(getIssueByKey('DEMO-1'))
addComment(getTodaysDate())



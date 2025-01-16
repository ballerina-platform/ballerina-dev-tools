import ballerinax/trigger.github;

listener github:Listener githubTestListener = new (listenerConfig = {webhookSecret: "secret"}, listenOn = 9090);

service github:IssuesService on githubTestListener {
    remote function onOpened(github:IssuesEvent payload) returns error? {
        do {
        } on fail error err {
            // handle error
        }
    }

    remote function onClosed(github:IssuesEvent payload) returns error? {
        do {
        } on fail error err {
            // handle error
        }
    }

    remote function onReopened(github:IssuesEvent payload) returns error? {
        do {
        } on fail error err {
            // handle error
        }
    }

    remote function onAssigned(github:IssuesEvent payload) returns error? {
        do {
        } on fail error err {
            // handle error
        }
    }

    remote function onLabeled(github:IssuesEvent payload) returns error? {
        do {
        } on fail error err {
            // handle error
        }
    }

    remote function onUnassigned(github:IssuesEvent payload) returns error? {
        do {
        } on fail error err {
            // handle error
        }
    }

    remote function onUnlabeled(github:IssuesEvent payload) returns error? {
        do {
        } on fail error err {
            // handle error
        }
    }
}

service github:IssueCommentService on githubTestListener {
    remote function onCreated(github:IssueCommentEvent payload) returns error? {
        do {
        } on fail error err {
            // handle error
        }
    }

    remote function onEdited(github:IssueCommentEvent payload) returns error? {
        do {
        } on fail error err {
            // handle error
        }
    }

    remote function onDeleted(github:IssueCommentEvent payload) returns error? {
        do {
        } on fail error err {
            // handle error
        }
    }
}

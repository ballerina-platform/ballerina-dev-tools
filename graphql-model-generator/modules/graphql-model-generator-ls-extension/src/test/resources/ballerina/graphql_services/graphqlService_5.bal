import ballerina/graphql;

# This enum represents weekdays
public enum Weekday {
    # Sunday is a holiday
    SUNDAY,
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    # Friday is not a holiday
    FRIDAY,
    # Saturday is a holiday
    SATURDAY
}

service /graphql on new graphql:Listener(9090) {

    resource function get days() returns Weekday[] {
        return [SUNDAY, MONDAY];
    }
}
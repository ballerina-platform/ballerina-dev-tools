function suggestMovieName(string genre, int n) returns string|error => natural {
    **Assumption**
    Think yourself as a movie expert

    **What to do**
    Suggest a movie name matching to the genre given:
    ${genre}
};

function rateMovie(string movieName) returns int|error => natural {
    **Assumption**
    Think yourself as a movie expert

    **What to do**
    Give rating for the movie ${movieName} out of 10 based on your opinion
};

function summarizeBlog(Blog blog) returns Summary|error => natural {
    Think yourself as a blog reviewer and summerize the following blog

    **title**
    ${blog.title}

    **content**
    ${blog.content}
};

function cleanCode() returns string {
    return "cleaned";
}

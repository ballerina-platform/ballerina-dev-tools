function suggestMovieName(string genre, int n) returns string|error => natural {
    **Assumption**
    Think yourself as a movie expert

    **What to do**
    Suggest a movie name matching to the genre given:
    ${genre}

    **Output**
    string - The suggested movie name
};

function rateMovie(string movieName) returns int|error => natural {
    **Assumption**
    Think yourself as a movie expert

    **What to do**
    Give rating for the movie ${movieName} out of 10 based on your opinion

    **Output**
    int - number between 1 and 10 as the rating
};

function summarizeBlog(Blog blog) returns Summary|error => natural {
    Think yourself as a blog reviewer and summerize the following blog

    **title**
    ${blog.title}

    **content**
    ${blog.content}
);

import ballerina/np;

function suggestMovieGenre(string input) returns string|error => natural {
    **Assumption**
    Think yourself as a movie expert

    **What to do**
    Suggest a movie genre matching to the input given:
    ${input}

    **Output**
    string - The suggested movie genre
};

function suggestMovieName1(string genre, int n) returns string|error => natural {
    **Assumption**
    Think yourself as a movie expert

    **What to do**
    Suggest a movie name matching to the genre given:
    ${genre}

    **Output**
    string - The suggested movie name
};

function suggestMovieName2() returns string|error => natural {
    **Assumption**
    Think yourself as a movie expert

    **What to do**
    Suggest a movie name

    **Output**
    string - The suggested movie name
};

function getMovieRating(np:ModelProvider model, string movieName) returns int|error => natural(model) {
    **Assumption**
    Think yourself as a movie expert

    **What to do**
    Give rating for the movie ${movieName} out of 10 based on your opinion

    **Output**
    int - number between 1 and 10 as the rating
};

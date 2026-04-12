# Project

The code for CS427 Android app.

## Gemini Key Setup

Set `GEMINI_KEY` in `Project/gradle.properties` (or your local gradle properties source) so
`BuildConfig.GEMINI_KEY` is populated at build time.
Do not hardcode API keys in Java source files.

## Team #304 member information

| Name | NetID | GitHub ID | Role | Experience |
|------|-------|-----------|------|------------|
| Shuyang (Emma) Zhang | sz94 | shuyangzhang-ai-sde | Tech lead | Full-stack, mobile app |
| Mahathi Jayaraman | mj45 | mahathij45 | Developer | Backend |
| Yuchen Wang | yuchen87 | yuchenwang3 | Developer | Backend AI/ML |
| Omeed Jamali | ojama2 | ojama2 | Manager | Backend, AI/ML |
| Akin Tuna Sakalli | sakalli2 | akinnsakalli | Developer | Backend, ML |
| Varsha Mullangi | varsham3 | varsham3 | Developer | Backend |
| Akshay Rao | Akshayr4 | Akreal1 | Developer | Backend |
| Lewen Shen | lewens2 | LeWEn22 | Developer | Backend |

## Weekly Progress Reports

**Meeting time:**
Wednesdays at 7:30pm central

**Meeting location (Zoom link):**
https://illinois.zoom.us/j/84553926777?pwd=kkjZmawBqyQAcdEbQJ40lr5Hb1oRiJ.1

Meeting ID: 845 5392 6777
Password: 138679

### Project Week 1

**Members present:**
sz94, sakalli2, ojama2, Akshayr4, mj45, yuchen87, varsham3

| NetID | Progress from last week | Tasks for next week |
|-------|-------------------------|---------------------|
| sz94 | Created the GitHub repository, added the TA and teammates as contributors, and created a Google Doc to draft the README tables. Handled and sent Team304-Iteration1.pdf to the TA in the Campuswire chat room. | Lead requirement brainstorming discussion. Ensure requirements align with app description. Draft initial UML class diagram. Assign use-case writing tasks to team members. |
| ojama2 | Created Discord group chat for communication, Set weekly meeting time, Created zoom link for weekly meeting | Lead weekly meeting. Check-in with team members to see if everyone was able to complete setup. Update weekly progress report. |
| sakalli2 | Set up the project environment | Attend weekly meeting and take on tasks |
| Akshayr4 | Set up the project environment | Engage in development based on group decisions starting next week |
| mj45 | Set up the project environment | Help with development based on initial draft of UML class diagram |
| varsham3 | Set up the project environment | Start development based on tasks assigned at the weekly meeting |
| yuchen87 | Set up the project environment | Contribute detailed requirements for the app's map and weather functionality |

### Project Week 2

**Members present:**
Akshayr4, mj45, yuchen87, ojama2, sz94, varsham3, sakalli2

**Meeting notes:**
During this weekly meeting, we went over some of the progress we made on iteration 2. At this point, we have finished the informal requirements and have divided the remainder of the tasks (UML Diagram, Transition Graph) to be completed next week.

| NetID | Progress from last week | Tasks for next week |
|-------|-------------------------|---------------------|
|Akshayr4| came up with 2 user flows|work on component transition graph|
|Mj45    | Came up with 2 fully dressed use cases|work on component transition graph|
|yuchen87| Contributed requirements for the app | Design UML class diagram for the app |
|ojama2| Came up with 2 informal requirements, Led weekly meeting | Work on UML class diagram for iteration 2 |
| sz94 | Understood Iteration 2 requirements. In the meeting, chose to write two informal requirements and UML. Completed two informal requirements in Week 2. | Take a look at all teammates' informal requirements and use cases, consider the UML and Component Transition Graph, then choose one area I am more skilled at to complete. Write peer review and update my section in README.md. |
|varsham3| came up with 2 fully dressed use cases for the app | begin working on component transition graph|
|       |                         |                     |
|       |                         |                     |
|       |                         |                     |

### Project Week 3

**Members present:**
ojama2, sz94, akshayr4, yuchen87, mj45, varsham3

**Meeting notes:**
This week, we completed all of the requirements of iteration 2 and combined our progress into a document ready for submission. We also started getting familiar with the tasks that we need to fulfill for week 3.

| NetID | Progress from last week | Tasks for next week |
|-------|-------------------------|---------------------|
| ojama2 | Worked on UML Class Diagram, Led weekly meeting | Begin looking through iteration 3 to discuss task assignment |
| akshayr4 | Worked on component transition graph | Get started on rough class structure |
| yuchen87 | Worked on UML Class Diagram | Understand starter codebase ahead of iteration 3 tasks |
| sz94 | Chose to work on UML. Collaborated with the teammate assigned to UML task, brainstormed, reviewed and refined details. Helped complete the PDF submitted to the TA. | Understand Iteration 3 requirements. |
| varsham3 | worked on component trasition graph | begin looking through iteration 3 requirements and brainstorm implementation possibilities|
| mj45      | Worked on component transition graph |Started brainstorming for iteration 3|
|       |                         |                     |
|       |                         |                     |

### Project Week 4

**Members present:**
ojama2, sz94, akshayr4, yuchen87, mj45, varsham3, sakalli2

**Meeting notes:**
This weekly meeting was used to divide tasks for the upcoming iteration 3 part 1 deadline based on which portions of the project people looked into and felt comfortable with.

| NetID | Progress from last week | Tasks for next week |
|-------|-------------------------|---------------------|
|   ojama2    |          Led weekly meeting, assigned tasks for iteration 3 part 1, began looking into LLM-driven UI customization               |           Implement LLM-driven UI customization          |
|   sz94    |           began looking into login/logout implementation              |          Implement login/logout           |
|   akshayr4    |          began looking into adding/removing cities               |          Implement add/remove cities           |
|   yuchen87    |             began looking into LLM-driven UI customization            |         Implement LLM-driven UI customization            |
|   mj45    |            began looking into adding/removing cities             |          Implement add/remove cities           |
|   varsham3    |            began looking into adding/removing cities             |         Implement add/remove cities            |
|   sakalli2    |           began looking into login/logout implementation              |          Implement login/logout           |
|       |                         |                     |

### Project Week 5

**Members present:**
ojama2, sz94, akshayr4, yuchen87, mj45, varsham3, sakalli2

**Meeting notes:**
This meeting was used to finalize changes for the iteration 3 part 1 submission. Merge conflicts were resolved and the app was thoroughly tested.

| NetID | Progress from last week | Tasks for next week |
|-------|-------------------------|---------------------|
|   ojama2    |          Implemented API Call and its processing to apply theme to MainActivity page               |         Begin looking into tasks for iteration 3 part 2            |
|   sz94    |              Implemented login/logout feature           |          Begin looking into tasks for iteration 3 part 2           |
|   akshayr4    |            Implemented add cities and backend for persistence             |         Begin looking into tasks for iteration 3 part 2            |
|   yuchen87    |          Created tools to parse API Call into JSON format for theme               |          Begin looking into tasks for iteration 3 part 2           |
|   mj45    |           Implemented remove cities and city verification              |          Begin looking into tasks for iteration 3 part 2           |
|    varsham3   |            Implemented remove cities and city verification             |          Begin looking into tasks for iteration 3 part 2           |
|    sakalli3   |           Implemented login/logout feature              |          Begin looking into tasks for iteration 3 part 2           |
|       |                         |                     |

### Project Week 6

**Members present:**
ojama2, sz94, akshayr4, yuchen87, mj45, varsham3, sakalli2

**Meeting notes:**
This meeting was used mainly to assign task for the current iteration and discuss implementation ideas and potential obstacles.

| NetID | Progress from last week | Tasks for next week |
|-------|-------------------------|---------------------|
|   ojama2    |          Led weekly meeting, assigned tasks for iteration 4, began looking into issues with theme persistence               |           Add theme persistence as user navigates through app, ensure additions align with rubric          |
|   sz94    |           began looking into weather details implementation              |          Implement weather details          |
|   akshayr4    |          began looking into adding interactive map               |          Implement interactive map           |
|   yuchen87    |             began looking into LLM-driven weather insights            |         Implement weather insights            |
|   mj45    |            began looking into adding interactive map               |          Implement interactive map           |
|   varsham3    |            began looking into adding interactive map               |          Implement interactive map           |
|   sakalli2    |           began looking into weather details implementation              |          Implement weather details          |

### Project Week 7

**Members present:**
ojama2, sz94, akshayr4, yuchen87, mj45, varsham3, sakalli2

**Meeting notes:**
This meeting was used to integrate additions made for iteration 4 and review comments left by the TA on the previous iteration. Merge conflicts were resolved and the app was thoroughly tested.

| NetID | Progress from last week | Tasks for next week |
|-------|-------------------------|---------------------|
|   ojama2    |          Added theme persistence across all pages, fixed formatting for city name display and weather insight response, led integration call               |         Begin looking into tasks for iteration 5            |
|   sz94    |              Implemented weather details           |          Begin looking into tasks for iteration 5           |
|   akshayr4    |            Implemented interactive map             |         Begin looking into tasks for iteration 5            |
|   yuchen87    |          Implemented prompt and response logic for weather insights               |          Begin looking into tasks for iteration 5           |
|   mj45    |           Implemented interactive map             |          Begin looking into tasks for iteration 5           |
|    varsham3   |            Implemented interactive map             |          Begin looking into tasks for iteration 5           |
|    sakalli3   |           Implemented weather details             |          Begin looking into tasks for iteration 5           |

### Project Week 8

**Members present:**
List the NetID of the people who attended the meeting

**Meeting notes:**
Explain the overall team progress and a summary of discussion

| NetID | Progress from last week | Tasks for next week |
|-------|-------------------------|---------------------|
|       |                         |                     |
|       |                         |                     |
|       |                         |                     |
|       |                         |                     |
|       |                         |                     |
|       |                         |                     |
|       |                         |                     |
|       |                         |                     |

### Project Week 9

**Members present:**
List the NetID of the people who attended the meeting

**Meeting notes:**
Explain the overall team progress and a summary of discussion

| NetID | Progress from last week | Tasks for next week |
|-------|-------------------------|---------------------|
|       |                         |                     |
|       |                         |                     |
|       |                         |                     |
|       |                         |                     |
|       |                         |                     |
|       |                         |                     |
|       |                         |                     |
|       |                         |                     |

### Project Week 10

**Members present:**
List the NetID of the people who attended the meeting

**Meeting notes:**
Explain the overall team progress and a summary of discussion

| NetID | Progress from last week | Tasks for next week |
|-------|-------------------------|---------------------|
|       |                         |                     |
|       |                         |                     |
|       |                         |                     |
|       |                         |                     |
|       |                         |                     |
|       |                         |                     |
|       |                         |                     |
|       |                         |                     |

### Project Week 11

**Members present:**
List the NetID of the people who attended the meeting

**Meeting notes:**
Explain the overall team progress and a summary of discussion

| NetID | Progress from last week | Tasks for next week |
|-------|-------------------------|---------------------|
|       |                         |                     |
|       |                         |                     |
|       |                         |                     |
|       |                         |                     |
|       |                         |                     |
|       |                         |                     |
|       |                         |                     |
|       |                         |                     |

### Project Week 12

**Members present:**
List the NetID of the people who attended the meeting

**Meeting notes:**
Explain the overall team progress and a summary of discussion

| NetID | Progress from last week | Tasks for next week |
|-------|-------------------------|---------------------|
|       |                         |                     |
|       |                         |                     |
|       |                         |                     |
|       |                         |                     |
|       |                         |                     |
|       |                         |                     |
|       |                         |                     |
|       |                         |                     |


import swaggerJsdoc from "swagger-jsdoc";
import swaggerUi from "swagger-ui-express";

const options = {
  definition: {
    openapi: "3.0.0",
    info: {
      title: "Modern Chore Management API",
      version: "1.0.0",
      description:
        "A modern chore management application with user authentication and real-time updates",
    },
    servers: [
      {
        url: "http://localhost:3000",
        description: "Development server",
      },
    ],
    components: {
      securitySchemes: {
        bearerAuth: {
          type: "http",
          scheme: "bearer",
          bearerFormat: "JWT",
        },
      },
      schemas: {
        User: {
          type: "object",
          properties: {
            id: {
              type: "integer",
              description: "User ID",
            },
            username: {
              type: "string",
              description: "Username",
            },
          },
        },
        Chore: {
          type: "object",
          properties: {
            id: {
              type: "integer",
              description: "Chore ID",
            },
            title: {
              type: "string",
              description: "Chore title",
            },
            description: {
              type: "string",
              description: "Chore description",
            },
            completed: {
              type: "boolean",
              description: "Completion status (deprecated, use status instead)",
            },
            status: {
              type: "string",
              enum: ["pending", "in-progress", "completed"],
              description: "Chore status",
            },
            priority: {
              type: "string",
              enum: ["low", "medium", "high"],
              description: "Chore priority",
            },
            dueDate: {
              type: "string",
              format: "date-time",
              description: "Due date",
            },
            points: {
              type: "integer",
              description: "Effort points for the chore",
            },
            createdAt: {
              type: "string",
              format: "date-time",
              description: "Creation timestamp",
            },
            updatedAt: {
              type: "string",
              format: "date-time",
              description: "Last update timestamp",
            },
            userId: {
              type: "integer",
              description: "Owner user ID",
            },
          },
        },
        Error: {
          type: "object",
          properties: {
            error: {
              type: "string",
              description: "Error message",
            },
          },
        },
      },
    },
  },
  apis: ["./src/routes/*.js"], // Path to the API docs
};

const specs = swaggerJsdoc(options);

export { specs, swaggerUi };

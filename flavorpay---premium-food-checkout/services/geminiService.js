
import { GoogleGenAI } from "@google/genai";

export class GeminiAssistant {
  constructor() {
    this.ai = new GoogleGenAI({ apiKey: process.env.API_KEY || '' });
  }

  async getTransactionAdvice(total, recentSpending) {
    try {
      const response = await this.ai.models.generateContent({
        model: 'gemini-3-flash-preview',
        contents: `I am at a checkout for a food order totaling $${total.toFixed(2)}. 
        My total food spending this month is $${recentSpending.toFixed(2)}. 
        Provide a very short, friendly, and helpful 1-sentence insight about my budget or the order. 
        Keep it concise and encouraging.`,
        config: {
          temperature: 0.7,
        }
      });
      return response.text;
    } catch (error) {
      console.error("Gemini Error:", error);
      return "You're within your average food budget. Enjoy your meal!";
    }
  }
}

export const geminiService = new GeminiAssistant();

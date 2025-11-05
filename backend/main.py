import os
from fastapi import FastAPI, File, UploadFile, HTTPException
from google.cloud import vision
import io

# Initialize FastAPI app
app = FastAPI()

# Initialize Google Cloud Vision client
# Make sure your Google Cloud credentials are set up in your environment
# For local development, you can use: export GOOGLE_APPLICATION_CREDENTIALS="path/to/your/keyfile.json"
vision_client = vision.ImageAnnotatorClient()

# In-memory "database" for storing recognized clothes
# For a real application, you would use a proper database like Firestore or Cloud SQL.
recognized_clothes = set()

@app.get("/")
def read_root():
    return {"message": "Welcome to the Uniform Distribution API"}

@app.post("/upload-image/")
async def upload_image(file: UploadFile = File(...)):
    """
    Receives an image, detects clothing items, and records new ones.
    """
    if not file.content_type.startswith("image/"):
        raise HTTPException(status_code=400, detail="File provided is not an image.")

    # Read image content
    content = await file.read()
    image = vision.Image(content=content)

    # Perform object localization
    try:
        response = vision_client.object_localization(image=image)
        localized_object_annotations = response.localized_object_annotations
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Google Cloud Vision API error: {e}")


    if response.error.message:
        raise HTTPException(status_code=500, detail=f"Vision API Error: {response.error.message}")

    newly_added_clothes = []
    existing_clothes_detected = []
    
    # List of terms to identify clothing items
    clothing_keywords = ["shirt", "t-shirt", "dress", "pants", "jeans", "sweater", "jacket", "coat", "shoe"]

    for obj in localized_object_annotations:
        # Check if the detected object is a piece of clothing
        is_clothing = any(keyword in obj.name.lower() for keyword in clothing_keywords)
        
        if is_clothing:
            # For simplicity, we use the object name as its identifier.
            # A more robust system might use features/hashes of the image crop.
            clothing_id = obj.name

            if clothing_id not in recognized_clothes:
                recognized_clothes.add(clothing_id)
                newly_added_clothes.append(clothing_id)
            else:
                existing_clothes_detected.append(clothing_id)

    return {
        "message": "Image processed successfully.",
        "total_clothes_detected": len(newly_added_clothes) + len(existing_clothes_detected),
        "newly_added_clothes": newly_added_clothes,
        "existing_clothes_detected": existing_clothes_detected,
        "all_recognized_clothes_in_system": list(recognized_clothes)
    }

if __name__ == "__main__":
    import uvicorn
    port = int(os.environ.get("PORT", 8080))
    uvicorn.run(app, host="0.0.0.0", port=port)

# IMPORTANT: TensorFlow Lite Model Files Required

This application requires the YamNet TensorFlow Lite model files to function.

## Required Files

You mentioned you have already downloaded these files. Please copy them to:

```
app/src/main/assets/models/yamnet.tflite
app/src/main/assets/models/yamnet_class_map.csv
```

### Where to get them:

1. **yamnet.tflite**: The desktop version uses `lite-model_yamnet_classification_tflite_1.tflite`
   - Rename it to `yamnet.tflite` and place in `app/src/main/assets/models/`
   - Download from: https://tfhub.dev/google/lite-model/yamnet/classification/tflite/1

2. **yamnet_class_map.csv**: The class labels CSV file
   - Should already be in your desktop project resources
   - Copy to `app/src/main/assets/models/`

## File Structure

```
app/
└── src/
    └── main/
        └── assets/
            └── models/
                ├── yamnet.tflite (required, ~5MB)
                └── yamnet_class_map.csv (required, ~12KB)
```

## Verification

After copying the files, verify they are in the correct location:
```bash
ls -lh app/src/main/assets/models/
```

You should see both files listed.
